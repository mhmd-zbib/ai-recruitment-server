package com.zbib.hiresync.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes or methods for logging via AOP.
 * Can be applied to classes or methods.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggableService {
    
    /**
     * Custom message to log. Can include parameter templates like ${paramName}.
     * These will be replaced with actual parameter values at runtime.
     */
    String message() default "";
    
    /**
     * Log level to use
     */
    LogLevel level() default LogLevel.INFO;
    
    /**
     * Whether to log method arguments
     */
    boolean logArguments() default true;
    
    /**
     * Whether to log the return value
     */
    boolean logReturnValue() default true;
    
    /**
     * Fields to mask for security/privacy
     */
    String[] sensitiveFields() default {
        "password", "token", "secret", "key", "credential", 
        "auth", "cookie", "ssn", "cardNumber", "cvv"
    };
    
    /**
     * Force logging even if level would normally be filtered out
     */
    boolean forceLogging() default false;
} 