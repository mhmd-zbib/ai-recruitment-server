package com.zbib.hiresync.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds a unique request ID to each request.
 * This ID is added to the MDC context for logging and to the response headers.
 */
@Component
public class RequestIdFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestId = UUID.randomUUID().toString();
        
        // Add request ID to MDC context for logging
        MDC.put("requestId", requestId);
        
        // Add correlation ID for tracking related operations
        MDC.put("correlationId", requestId);
        
        // Add user ID if available (from security context)
        // This would be populated by your authentication mechanism
        
        // Log the incoming request
        logger.info("Received request: {} {} from {}", 
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr());

        try {
            // Add request ID to response headers
            ((HttpServletResponse) response).setHeader("X-Request-ID", requestId);
            
            // Continue with the filter chain
            chain.doFilter(request, response);
        } finally {
            // Clear the MDC context to prevent memory leaks
            MDC.clear();
        }
    }
}
