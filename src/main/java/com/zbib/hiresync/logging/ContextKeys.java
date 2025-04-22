package com.zbib.hiresync.logging;

/**
 * Centralized constants for Log4j2 ThreadContext keys.
 * Used throughout the application for structured logging.
 */
public final class ContextKeys {

    // Request identification
    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    
    // User identification
    public static final String USER_ID = "userId";
    public static final String ROLES = "roles";
    public static final String SESSION_ID = "sessionId";
    
    // Request metadata
    public static final String REQUEST_PATH = "requestPath";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String SOURCE_IP = "sourceIp";
    public static final String USER_AGENT = "userAgent";
    
    // Response information
    public static final String STATUS = "status";
    public static final String DURATION = "duration";
    public static final String EXECUTION_TIME = "executionTime";
    
    // Error information
    public static final String EXCEPTION = "exception";
    public static final String ERROR_MESSAGE = "errorMessage";
    
    // Method execution context
    public static final String CLASS = "class";
    public static final String METHOD = "method";
    
    // Business context
    public static final String OPERATION = "operation";
    
    // Metadata prefix
    public static final String META_PREFIX = "meta.";
    
    private ContextKeys() {
        // Prevent instantiation
    }
} 