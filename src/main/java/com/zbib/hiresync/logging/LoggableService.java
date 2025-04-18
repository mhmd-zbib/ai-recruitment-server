package com.zbib.hiresync.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables logging for methods or classes
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggableService {
    
    /**
     * Log level to use
     */
    LogLevel level() default LogLevel.INFO;
 
    /**
     * Whether to log method arguments
     */
    boolean logArguments() default true;
  
    /**
     * List of field names that should be masked
     */
    String[] sensitiveFields() default {"password", "token", "secret", "key", "credential"};
    
    /**
     * Custom message to log (supports placeholders like ${paramName})
     */
    String message() default "";
}