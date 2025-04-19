package com.zbib.hiresync.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple HTTP request logger that captures basic request metrics
 */
@Component
@Order(10) // Runs after LoggingContextFilter
@ConditionalOnProperty(name = "hiresync.logging.request-logging-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RequestLogger extends OncePerRequestFilter {
    private static final Logger logger = LogManager.getLogger(RequestLogger.class);
    private static final Set<String> DEFAULT_EXCLUDED_PATHS = new HashSet<>(
            Arrays.asList("actuator", "health", "metrics", "static", "favicon.ico"));
    
    private final MaskingUtils maskingUtils;
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return DEFAULT_EXCLUDED_PATHS.stream().anyMatch(path::contains);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // Create stopwatch for timing
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // Log request
        if (logger.isInfoEnabled()) {
            logRequest(request);
        }
        
        try {
            // Execute the chain
            chain.doFilter(request, response);
        } finally {
            // Stop timing
            stopWatch.stop();
            long duration = stopWatch.getTotalTimeMillis();
            
            // Update context with response data
            ThreadContext.put(ContextKeys.STATUS, String.valueOf(response.getStatus()));
            ThreadContext.put(ContextKeys.DURATION, String.valueOf(duration));
            
            // Log response
            logResponse(request, response, duration);
        }
    }
    
    private void logRequest(HttpServletRequest request) {
        String queryString = request.getQueryString() != null ? "?" + maskingUtils.mask(request.getQueryString()) : "";
        String clientIp = request.getRemoteAddr();
        
        logger.info("Request: {} {} {} from IP: {}", 
                request.getMethod(),
                request.getRequestURI(),
                queryString,
                clientIp);
    }
    
    private void logResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
        int status = response.getStatus();
        
        // Log at appropriate level based on status code
        if (status >= 500) {
            logger.error("Response: {} {} completed in {}ms with error status {}", 
                    request.getMethod(), request.getRequestURI(), duration, status);
        } else if (status >= 400) {
            logger.warn("Response: {} {} completed in {}ms with client error status {}", 
                    request.getMethod(), request.getRequestURI(), duration, status);
        } else {
            logger.info("Response: {} {} completed in {}ms with status {}", 
                    request.getMethod(), request.getRequestURI(), duration, status);
        }
    }
} 