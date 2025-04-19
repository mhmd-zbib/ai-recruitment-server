package com.zbib.hiresync.logging;

/**
 * Centralized constants for MDC context keys.
 * <p>
 * This class provides a single source of truth for all context key names
 * used in the application's logging framework. Using these constants ensures
 * consistency in key naming across the entire application.
 */
public final class ContextKeys {
    // Request identification
    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    
    // User and session details
    public static final String USER_ID = "userId";
    public static final String ROLES = "roles";
    public static final String SESSION_ID = "sessionId";
    
    // Request metadata
    public static final String REQUEST_PATH = "path";
    public static final String REQUEST_METHOD = "method";
    public static final String SOURCE_IP = "clientIp";
    public static final String USER_AGENT = "userAgent";
    
    // Status and metrics
    public static final String STATUS = "status";
    public static final String DURATION = "duration";
    public static final String EXECUTION_TIME = "executionTime";
    
    // Error details
    public static final String EXCEPTION = "exception";
    public static final String ERROR_MESSAGE = "errorMessage";
    
    // Request/response content
    public static final String HEADERS = "headers";
    public static final String REQUEST_BODY = "requestBody";
    public static final String RESPONSE_BODY = "responseBody";
    
    // Method execution context
    public static final String CLASS = "class";
    public static final String METHOD = "method";
    public static final String ARGUMENTS = "arguments";
    
    // Business context
    public static final String TENANT_ID = "tenantId";
    public static final String RESOURCE_ID = "resourceId";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String OPERATION = "operation";
    public static final String META_PREFIX = "meta.";
    
    // Prevent instantiation
    private ContextKeys() {
        throw new AssertionError("Utility class should not be instantiated");
    }
} 