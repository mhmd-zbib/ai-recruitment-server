package com.zbib.hiresync.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Filter that logs HTTP requests and responses
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConsolidatedRequestLogger extends OncePerRequestFilter {

    private static final Logger LOGGER = LogManager.getLogger(ConsolidatedRequestLogger.class);
    private static final String CORRELATION_ID = "correlationId";
    private static final int MAX_CONTENT_LENGTH = 500;
    
    private final Set<String> excludedPaths = new HashSet<>(Arrays.asList(
        "/actuator", 
        "/health",  
        "/metrics", 
        "/static", 
        "/resources",
        "/favicon.ico"
    ));
    
    private final MaskingUtils maskingUtils;
    
    @Value("${hiresync.logging.log-request-body:false}")
    private boolean logRequestBody;
    
    @Value("${hiresync.logging.log-response-body:false}")
    private boolean logResponseBody;
    
    public ConsolidatedRequestLogger(MaskingUtils maskingUtils) {
        this.maskingUtils = maskingUtils;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludedPaths.stream().anyMatch(pattern -> path.startsWith(pattern));
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if (request instanceof ContentCachingRequestWrapper) {
            filterChain.doFilter(request, response);
            return;
        }
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        setupContext(request);
        
        try {
            long startTime = System.currentTimeMillis();
            
            LOGGER.info("{} request started: {}", request.getMethod(), request.getRequestURI());
            
            filterChain.doFilter(requestWrapper, responseWrapper);
            
            long duration = System.currentTimeMillis() - startTime;
            ThreadContext.put("executionTime", duration + "ms");
            ThreadContext.put("statusCode", String.valueOf(responseWrapper.getStatus()));
            
            logRequestCompletion(requestWrapper, responseWrapper, duration);
            
        } finally {
            responseWrapper.copyBodyToResponse();
            ThreadContext.clearAll();
        }
    }
    
    private void setupContext(HttpServletRequest request) {
        String correlationId = ThreadContext.get(CORRELATION_ID);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = "TX-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        ThreadContext.clearAll();
        ThreadContext.put(CORRELATION_ID, correlationId);
        ThreadContext.put("httpMethod", request.getMethod());
        ThreadContext.put("apiPath", request.getRequestURI());
        ThreadContext.put("clientIP", getClientIp(request));
        
        String userId = request.getHeader("X-User-ID");
        if (userId != null) {
            ThreadContext.put("userId", userId);
        }
    }
    
    private void logRequestCompletion(ContentCachingRequestWrapper request, 
                            ContentCachingResponseWrapper response, 
                            long duration) {
        int status = response.getStatus();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        StringBuilder message = new StringBuilder()
            .append(method)
            .append(" ")
            .append(uri)
            .append(" completed in ")
            .append(duration)
            .append("ms with status ")
            .append(status);
        
        // Add request/response bodies for debugging if needed and enabled
        if (shouldLogBody(request, response)) {
            appendRequestResponseBodies(message, request, response);
        }
        
        if (status >= 500) {
            // Only log server errors (5xx) as ERROR
            LOGGER.error(message.toString());
        } else if (status >= 400 || duration > 1000) {
            // Log client errors (4xx) and slow responses as WARN
            LOGGER.warn(message.toString());
        } else {
            LOGGER.info(message.toString());
        }
    }
    
    private boolean shouldLogBody(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        // Only log bodies for non-successful responses or if explicitly enabled
        int status = response.getStatus();
        return (status >= 400 && (logRequestBody || logResponseBody)) || 
               (logRequestBody && logResponseBody);
    }
    
    private void appendRequestResponseBodies(StringBuilder message, 
                                 ContentCachingRequestWrapper request, 
                                 ContentCachingResponseWrapper response) {
        try {
            // Add request body if enabled and present
            if (logRequestBody) {
                byte[] content = request.getContentAsByteArray();
                if (content.length > 0) {
                    String requestBody = getContentAsString(content, request.getCharacterEncoding());
                    if (isJsonContentType(request.getContentType())) {
                        requestBody = maskingUtils.maskSensitiveData(requestBody);
                    }
                    message.append("\nRequest: ").append(truncate(requestBody));
                }
            }
            
            // Add response body if enabled and present
            if (logResponseBody) {
                byte[] content = response.getContentAsByteArray();
                if (content.length > 0) {
                    String responseBody = getContentAsString(content, response.getCharacterEncoding());
                    if (isJsonContentType(response.getContentType())) {
                        responseBody = maskingUtils.maskSensitiveData(responseBody);
                    }
                    message.append("\nResponse: ").append(truncate(responseBody));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not log request/response bodies", e);
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String headerValue = request.getHeader("X-Forwarded-For");
        if (headerValue != null) {
            return headerValue.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("json");
    }
    
    private String getContentAsString(byte[] content, String encoding) 
            throws UnsupportedEncodingException {
        if (content.length == 0) return "";
        return new String(content, encoding != null ? encoding : "UTF-8");
    }
    
    private String truncate(String content) {
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_LENGTH) + "...";
    }
} 