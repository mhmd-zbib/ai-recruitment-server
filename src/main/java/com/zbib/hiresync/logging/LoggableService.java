package com.zbib.hiresync.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for enabling automatic method logging.
 * <p>
 * When applied to a class or method, this annotation triggers AOP-based
 * logging of method entry, exit, execution time, and (optionally) arguments.
 * <p>
 * Example usage:
 * <pre>
 * {@code @LoggableService(level = LogLevel.DEBUG, logArguments = true)}
 * public User findUserById(Long id) { ... }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggableService {
    
    /**
     * Log level to use for generated log messages.
     */
    LogLevel level() default LogLevel.INFO;
 
    /**
     * Whether to include method arguments in log messages.
     * When true, arguments will be logged with sensitive data masked.
     */
    boolean logArguments() default true;
  
    /**
     * List of field or parameter names that should be masked in logs.
     * These names are matched case-insensitively and as substrings.
     */
    String[] sensitiveFields() default {
        "password", "token", "secret", "key", "credential", 
        "auth", "ssn", "creditcard", "card", "cvv", "ssn"
    };
    
    /**
     * Custom message to include in log entries.
     * <p>
     * Supports placeholder substitution using ${paramName} syntax, 
     * which will be replaced with actual parameter values.
     */
    String message() default "";
}