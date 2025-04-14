package com.zbib.hiresync.logging;

/**
 * Enumeration of logging levels for service logging.
 * Maps to standard Log4j2 levels for consistent logging across the application.
 */
public enum LogLevel {
    /**
     * Detailed debugging information, typically disabled in production.
     */
    DEBUG,
    
    /**
     * Informational messages highlighting progress of the application.
     */
    INFO,
    
    /**
     * Potentially harmful situations that might need attention.
     */
    WARN,
    
    /**
     * Error events that might still allow the application to continue running.
     */
    ERROR,
    
    /**
     * Very severe error events that will presumably lead the application to abort.
     */
    FATAL
} 