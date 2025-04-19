package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HTTP request/response logger that captures and masks request/response details
 * while leveraging MDC context from MdcContextFilter
 */
@Component
@Order(10) // Runs after MdcContextFilter
@ConditionalOnProperty(name = "hiresync.logging.request-logging-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ConsolidatedRequestLogger extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(ConsolidatedRequestLogger.class);
    
    @Value("${hiresync.logging.excluded-paths:actuator,health,metrics,static,favicon.ico}")
    private String excludedPathsString;
    
    @Value("${hiresync.logging.log-request-body:true}")
    private boolean logRequestBody;
    
    @Value("${hiresync.logging.log-response-body:true}")
    private boolean logResponseBody;
    
    @Value("${hiresync.logging.log-headers:true}")
    private boolean logHeaders;
    
    @Value("${hiresync.logging.slow-request-threshold-ms:1000}")
    private long slowRequestThreshold;
    
    private final ObjectMapper objectMapper;
    private final MaskingUtils maskingUtils;
    private final HttpContentProcessor contentProcessor;
    private Set<String> excludedPaths;
    
    @Override
    protected void initFilterBean() {
        this.excludedPaths = initializeExcludedPaths();
    }
    
    private Set<String> initializeExcludedPaths() {
        Set<String> paths = new HashSet<>();
        if (excludedPathsString != null && !excludedPathsString.isEmpty()) {
            paths.addAll(Arrays.asList(excludedPathsString.split(",\\s*")));
        }
        return paths;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludedPaths.stream().anyMatch(path::contains);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // Avoid double-wrapping if already wrapped
        if (request instanceof ContentCachingRequestWrapper) {
            chain.doFilter(request, response);
            return;
        }
        
        // Create wrappers that cache request and response bodies
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        // Create stopwatch for timing
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("request");
        
        try {
            // Log request
            if (logger.isInfoEnabled()) {
                logRequest(requestWrapper);
            }
            
            // Execute the rest of the filter chain
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            // Stop timing and log response
            stopWatch.stop();
            long duration = stopWatch.getTotalTimeMillis();
            
            // Update MDC context with response data
            MdcContext.put(ContextKeys.STATUS, String.valueOf(responseWrapper.getStatus()));
            MdcContext.put(ContextKeys.DURATION, String.valueOf(duration));
            
            // Log the response
            logResponse(requestWrapper, responseWrapper, duration);
            
            // Copy content to the original response
            responseWrapper.copyBodyToResponse();
        }
    }
    
    private void logRequest(ContentCachingRequestWrapper request) {
        Map<String, Object> requestLog = new HashMap<>();
        requestLog.put("method", request.getMethod());
        requestLog.put("uri", request.getRequestURI());
        
        if (request.getQueryString() != null) {
            requestLog.put("queryString", request.getQueryString());
        }
        
        requestLog.put("clientIp", contentProcessor.extractClientIp(request));
        
        // Add headers if enabled
        if (logHeaders) {
            Map<String, String> headers = contentProcessor.extractRequestHeaders(request);
            if (!headers.isEmpty()) {
                requestLog.put("headers", headers);
            }
        }
        
        // Log request body if enabled and present
        if (logRequestBody && request.getContentLength() > 0) {
            requestLog.put("body", "[body will be logged after request]");
        }
        
        logger.info("Incoming request: {}", maskingUtils.maskObject(requestLog));
    }
    
    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        Map<String, Object> responseLog = new HashMap<>();
        
        // Basic request/response info
        responseLog.put("method", request.getMethod());
        responseLog.put("uri", request.getRequestURI());
        if (request.getQueryString() != null) {
            responseLog.put("queryString", request.getQueryString());
        }
        
        // Status info
        responseLog.put("status", status);
        responseLog.put("statusText", HttpStatus.valueOf(status).getReasonPhrase());
        responseLog.put("duration", duration + "ms");
        
        // Add headers if enabled
        if (logHeaders) {
            Map<String, String> headers = contentProcessor.extractResponseHeaders(response);
            if (!headers.isEmpty()) {
                responseLog.put("headers", headers);
            }
        }
        
        // Add request body if enabled and present
        if (logRequestBody && request.getContentLength() > 0) {
            String requestBody = contentProcessor.getRequestBody(request);
            if (requestBody != null && !requestBody.isEmpty()) {
                responseLog.put("requestBody", contentProcessor.processContent(request.getContentType(), requestBody));
            }
        }
        
        // Add response body if enabled and present
        if (logResponseBody && response.getContentSize() > 0) {
            String responseBody = contentProcessor.getResponseBody(response);
            if (responseBody != null && !responseBody.isEmpty()) {
                responseLog.put("responseBody", contentProcessor.processContent(response.getContentType(), responseBody));
            }
        }
        
        // Log at appropriate level based on status code and duration
        String maskedLog = maskingUtils.maskObject(responseLog);
        if (status >= 500) {
            logger.error("Server error response: {}", maskedLog);
        } else if (status >= 400 || duration > slowRequestThreshold) {
            logger.warn("Client error response: {}", maskedLog);
        } else {
            logger.info("Response: {}", maskedLog);
        }
    }
} 