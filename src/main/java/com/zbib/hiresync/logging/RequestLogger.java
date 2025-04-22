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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Standardized HTTP request logger that provides a single, comprehensive log entry per API call
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "hiresync.logging.request-logging-enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class RequestLogger extends OncePerRequestFilter {
    private static final Logger logger = LogManager.getLogger(RequestLogger.class);
    private static final Set<String> DEFAULT_EXCLUDED_PATHS = new HashSet<>(
            Arrays.asList("actuator", "health", "metrics", "static", "favicon.ico"));
    
    private static final int MAX_PAYLOAD_LENGTH = 2000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    // Fields that should be masked in logs
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(
            Arrays.asList("password", "token", "secret", "key", "credential", "auth", "ssn", "creditcard", "card", "cvv"));
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return DEFAULT_EXCLUDED_PATHS.stream().anyMatch(path::contains);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // Wrap request and response for content caching
        ContentCachingRequestWrapper requestWrapper = request instanceof ContentCachingRequestWrapper ?
                (ContentCachingRequestWrapper) request : new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = response instanceof ContentCachingResponseWrapper ?
                (ContentCachingResponseWrapper) response : new ContentCachingResponseWrapper(response);
        
        // Generate a correlation ID for this request
        String correlationId = UUID.randomUUID().toString();
        ThreadContext.put("correlationId", correlationId);
        
        // Create stopwatch for timing
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // Track any potential exceptions
        Throwable error = null;
        
        try {
            // Execute the chain without logging request start
            chain.doFilter(requestWrapper, responseWrapper);
        } catch (Throwable ex) {
            // Capture exception for logging
            error = ex;
            throw ex;
        } finally {
            // Stop timing
            stopWatch.stop();
            long duration = stopWatch.getTotalTimeMillis();
            
            // If error wasn't caught directly, check the request attributes for error
            if (error == null) {
                error = (Throwable) request.getAttribute("org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR");
            }
            
            // Log single entry with all information
            logRequestAndResponse(requestWrapper, responseWrapper, duration, correlationId, error);
            
            // Make sure the response is committed
            responseWrapper.copyBodyToResponse();
            
            // Clean up context
            ThreadContext.remove("correlationId");
        }
    }
    
    private void logRequestAndResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, 
                                      long duration, String correlationId, Throwable error) {
        // Extract standard fields
        String timestamp = ZonedDateTime.now(ZoneId.of("UTC")).format(DATE_FORMATTER);
        int status = response.getStatus();
        String userId = extractUserId();
        String ip = extractIpAddress(request);
        String serviceName = extractServiceName(request.getRequestURI());
        String operation = getMessageFromPath(request.getRequestURI());
        String message = determineMessage(serviceName, operation, error, status);
        
        // Basic log entry for success cases
        if (status < 400) {
            logger.info("[{}] INFO {} - {}\nUser ID     : {}\nIP          : {}\nRequest ID  : {}\nHTTP Status : {}\nDuration    : {}ms", 
                    timestamp, serviceName, message, userId != null ? userId : "Anonymous", ip, correlationId, 
                    status + " (" + getStatusText(status) + ")", duration);
            return;
        }
        
        // For client errors (4xx)
        if (status >= 400 && status < 500) {
            logger.warn("[{}] WARN {} - {}\nUser ID     : {}\nIP          : {}\nRequest ID  : {}\nHTTP Status : {}\nException   : {}\nDuration    : {}ms", 
                    timestamp, serviceName, message, userId != null ? userId : "Anonymous", ip, correlationId, 
                    status + " (" + getStatusText(status) + ")", 
                    error != null ? error.getClass().getSimpleName() : "ClientError", 
                    duration);
            return;
        }
        
        // For server errors (5xx)
        StringBuilder errorLog = new StringBuilder();
        errorLog.append(String.format("[%s] ERROR %s - %s%n", timestamp, serviceName, message));
        errorLog.append(String.format("User ID     : %s%n", userId != null ? userId : "Anonymous"));
        errorLog.append(String.format("IP          : %s%n", ip));
        errorLog.append(String.format("Request ID  : %s%n", correlationId));
        errorLog.append(String.format("HTTP Status : %s (%s)%n", status, getStatusText(status)));
        
        if (error != null) {
            errorLog.append(String.format("Exception   : %s%n", error.getClass().getSimpleName()));
        }
        
        errorLog.append(String.format("Duration    : %dms%n", duration));
        
        // Add request data for 500 errors
        if (status >= 500) {
            // Add input data
            errorLog.append("\nRequest Data:");
            errorLog.append(String.format("\nMethod      : %s %s", request.getMethod(), request.getRequestURI()));
            
            // Add query parameters if present
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                errorLog.append(String.format("\nQuery       : %s", maskSensitiveData(queryString)));
            }
            
            // Add headers (selective)
            errorLog.append("\nHeaders     :");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Skip common headers that add noise
                if (!headerName.equalsIgnoreCase("cookie") && 
                    !headerName.equalsIgnoreCase("authorization") &&
                    !headerName.startsWith("sec-")) {
                    errorLog.append(String.format("\n  %s: %s", headerName, request.getHeader(headerName)));
                }
            }
            
            // Add request body for certain content types
            if (shouldLogRequestBody(request)) {
                String requestBody = extractRequestBody(request);
                if (requestBody != null && !requestBody.isEmpty()) {
                    errorLog.append(String.format("\nBody        : %s", maskSensitiveData(requestBody)));
                }
            }
            
            // Add stack trace for errors
            if (error != null) {
                errorLog.append("\n\nStack Trace:");
                errorLog.append(extractStackTrace(error, 20));
            }
        }
        
        logger.error(errorLog.toString());
    }
    
    /**
     * Get a human-readable status text for HTTP status codes
     */
    private String getStatusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> String.valueOf(status);
        };
    }
    
    /**
     * Determine an appropriate message for the log based on the context
     */
    private String determineMessage(String service, String operation, Throwable error, int status) {
        if (error != null) {
            if (status >= 500) {
                return operation + " failed";
        } else {
                return operation + " rejected";
            }
        }
        
        if (status >= 400) {
            return operation + " failed";
        }
        
        if (operation != null) {
            if (status == 201) {
                return operation + " succeeded (created)";
            }
            return operation + " succeeded";
        }
        
        return "Request processed";
    }
    
    /**
     * Extract client IP address, handling proxies
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // If there are multiple IPs in X-Forwarded-For, take the first one (client IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }
    
    /**
     * Determine if we should log the request body based on content type
     */
    private boolean shouldLogRequestBody(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        return contentType != null && 
               (contentType.contains("application/json") || 
                contentType.contains("application/xml") ||
                contentType.contains("application/x-www-form-urlencoded") ||
                contentType.contains("text/"));
    }
    
    /**
     * Extract request body as string
     */
    private String extractRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        
        int length = Math.min(content.length, MAX_PAYLOAD_LENGTH);
        try {
            return new String(content, 0, length, request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            return new String(content, 0, length, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Mask sensitive data in JSON or string content
     */
    private String maskSensitiveData(String content) {
        if (content == null) {
            return null;
        }
        
        String maskedContent = content;
        
        // Simple regex-based masking for sensitive fields
        for (String field : SENSITIVE_FIELDS) {
            // Match patterns like "password": "secret" or "password":"secret"
            maskedContent = maskedContent.replaceAll(
                "\"" + field + "\"\\s*:\\s*\"[^\"]*\"",
                "\"" + field + "\": \"********\""
            );
            
            // Also match URL parameters like password=secret
            maskedContent = maskedContent.replaceAll(
                field + "=[^&]+",
                field + "=********"
            );
        }
        
        return maskedContent;
    }
    
    /**
     * Extract formatted stack trace
     */
    private String extractStackTrace(Throwable error, int maxLines) {
        if (error == null || error.getStackTrace() == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elements = error.getStackTrace();
        
        int lines = Math.min(elements.length, maxLines);
        for (int i = 0; i < lines; i++) {
            sb.append("\n    at ").append(elements[i].toString());
        }
        
        if (elements.length > maxLines) {
            sb.append("\n    ... ").append(elements.length - maxLines).append(" more");
        }
        
        // Add caused-by chain
        Throwable cause = error.getCause();
        if (cause != null) {
            sb.append("\nCaused by: ").append(cause.getClass().getName());
            if (cause.getMessage() != null) {
                sb.append(": ").append(cause.getMessage());
            }
            
            if (cause.getStackTrace() != null && cause.getStackTrace().length > 0) {
                sb.append("\n    at ").append(cause.getStackTrace()[0].toString());
                if (cause.getStackTrace().length > 1) {
                    sb.append("\n    ... ").append(cause.getStackTrace().length - 1).append(" more");
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Extracts a human-readable service name from the request path
     */
    private String extractServiceName(String path) {
        if (path == null || path.isEmpty()) {
            return "Unknown";
        }
        
        // Remove API version prefix if present
        String cleanPath = path.replaceAll("^/api(/v[0-9]+)?", "");
        
        // Split the path to get main service name
        String[] parts = cleanPath.split("/");
        if (parts.length > 1) {
            // Convert auth-service to AuthService
            String serviceName = parts[1];
            if (serviceName.contains("-")) {
                serviceName = Arrays.stream(serviceName.split("-"))
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                        .collect(java.util.stream.Collectors.joining());
            } else {
                serviceName = serviceName.substring(0, 1).toUpperCase() + serviceName.substring(1);
            }
            return serviceName + "Service";
        }
        
        return "MainService";
    }
    
    /**
     * Creates a human-readable message from the request path
     */
    private String getMessageFromPath(String path) {
        // Extract operation from path
        if (path.contains("/login")) {
            return "User login";
        } else if (path.contains("/signup") || path.contains("/register")) {
            return "User registration";
        } else if (path.contains("/refresh")) {
            return "Token refresh";
        } else if (path.contains("/logout")) {
            return "User logout";
        } else if (path.contains("/session")) {
            return "Session management";
        } else if (path.endsWith("/")) {
            return "List request";
        } else if (path.matches(".*/[a-zA-Z0-9-]+$")) {
            return "Get item";
        } else if (path.contains("search")) {
            return "Search request";
        }
        
        // Default message
        String[] parts = path.split("/");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            return "Request to " + lastPart;
        }
        
        return "API request";
    }
    
    /**
     * Gets the current user ID from the security context
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().toString().equals("anonymousUser")) {
            return authentication.getName();
        }
        return null;
    }
} 