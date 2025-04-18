package com.zbib.hiresync.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * HTTP request/response logger
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "hiresync.logging.request-logging-enabled", havingValue = "true", matchIfMissing = true)
public class ConsolidatedRequestLogger extends OncePerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(ConsolidatedRequestLogger.class);
    private static final String CORRELATION_ID = "correlationId";
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie", "token");
    
    @Value("${hiresync.logging.max-content-length:1000}")
    private int maxContentLength;
    
    @Value("${hiresync.logging.excluded-paths:actuator,health,metrics,static,favicon.ico}")
    private String excludedPathsString;
    
    @Value("${hiresync.logging.log-request-body:false}")
    private boolean logRequestBody;
    
    @Value("${hiresync.logging.log-response-body:false}")
    private boolean logResponseBody;
    
    @Value("${hiresync.logging.log-headers:false}")
    private boolean logHeaders;
    
    @Value("${hiresync.logging.slow-request-threshold-ms:1000}")
    private long slowRequestThreshold;
    
    private final MaskingUtils maskingUtils;
    private final UserIdentifierProvider userIdentifierProvider;
    private Set<String> excludedPaths;
    
    public ConsolidatedRequestLogger(MaskingUtils maskingUtils, UserIdentifierProvider userIdentifierProvider) {
        this.maskingUtils = maskingUtils;
        this.userIdentifierProvider = userIdentifierProvider;
    }
    
    @Override
    protected void initFilterBean() {
        this.excludedPaths = new HashSet<>();
        if (excludedPathsString != null && !excludedPathsString.isEmpty()) {
            this.excludedPaths.addAll(Arrays.asList(excludedPathsString.split(",\\s*")));
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludedPaths.stream().anyMatch(path::startsWith);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        if (request instanceof ContentCachingRequestWrapper) {
            chain.doFilter(request, response);
            return;
        }
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        setupRequestContext(requestWrapper);
        
        long startTime = System.currentTimeMillis();
        LOG.info("{} {} started", request.getMethod(), request.getRequestURI());
        
        try {
            chain.doFilter(requestWrapper, responseWrapper);
            
            String userId = userIdentifierProvider.getCurrentUserId();
            if (userId != null) {
                ThreadContext.put("userId", userId);
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestCompletion(requestWrapper, responseWrapper, duration);
            
            responseWrapper.copyBodyToResponse();
            ThreadContext.clearAll();
        }
    }
    
    private void setupRequestContext(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }
        ThreadContext.put(CORRELATION_ID, correlationId);
        
        ThreadContext.put("method", request.getMethod());
        ThreadContext.put("uri", request.getRequestURI());
        ThreadContext.put("ip", getClientIp(request));
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            ThreadContext.put("userAgent", userAgent);
        }
        
        if (logHeaders) {
            addRequestHeaders(request);
        }
    }
    
    private void logRequestCompletion(ContentCachingRequestWrapper request, 
            ContentCachingResponseWrapper response, long duration) {
        
        int status = response.getStatus();
        
        ThreadContext.put("status", String.valueOf(status));
        ThreadContext.put("duration", String.valueOf(duration));
        
        if (logRequestBody || logResponseBody) {
            logBodies(request, response);
        }
        
        StringBuilder message = new StringBuilder()
            .append(request.getMethod()).append(' ')
            .append(request.getRequestURI());
        
        if (request.getQueryString() != null) {
            message.append('?').append(maskingUtils.mask(request.getQueryString()));
        }
        
        message.append(" [").append(status).append("] ")
               .append(duration).append("ms");
        
        if (status >= 500) {
            LOG.error(message.toString());
        } else if (status >= 400 || duration > slowRequestThreshold) {
            LOG.warn(message.toString());
        } else {
            LOG.info(message.toString());
        }
    }
    
    private void addRequestHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            if (headers.length() > 0) {
                headers.append(", ");
            }
            
            String value = request.getHeader(name);
            if (isSensitiveHeader(name)) {
                value = maskingUtils.mask(value);
            }
            
            headers.append(name).append('=').append(value);
        });
        
        if (headers.length() > 0) {
            ThreadContext.put("headers", headers.toString());
        }
    }
    
    private boolean isSensitiveHeader(String name) {
        String lowerName = name.toLowerCase();
        return SENSITIVE_HEADERS.contains(lowerName) || maskingUtils.isSensitive(lowerName);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
    
    private void logBodies(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        if (logRequestBody && request.getContentAsByteArray().length > 0) {
            String content = getBodyAsString(request.getContentAsByteArray(), request.getCharacterEncoding());
            
            if (isJsonContent(request.getContentType())) {
                content = maskingUtils.mask(content);
            }
            
            ThreadContext.put("requestBody", truncate(content));
        }
        
        if (logResponseBody && response.getContentAsByteArray().length > 0) {
            String content = getBodyAsString(response.getContentAsByteArray(), response.getCharacterEncoding());
            
            if (isJsonContent(response.getContentType())) {
                content = maskingUtils.mask(content);
            }
            
            ThreadContext.put("responseBody", truncate(content));
        }
    }
    
    private String getBodyAsString(byte[] content, String encoding) {
        String charset = encoding != null ? encoding : StandardCharsets.UTF_8.name();
        try {
            return new String(content, charset);
        } catch (Exception e) {
            return "[Error reading body]";
        }
    }
    
    private boolean isJsonContent(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("json");
    }
    
    private String truncate(String content) {
        if (content == null || content.length() <= maxContentLength) {
            return content;
        }
        return content.substring(0, maxContentLength) + "... [truncated]";
    }
} 