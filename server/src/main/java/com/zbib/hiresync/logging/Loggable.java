package com.zbib.hiresync.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that should be logged.
 * This annotation is used to mark methods for which entry, exit, arguments, and execution time should be logged.
 * It can be configured with a custom message, log level, and other options.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    /**
     * Custom message to be included in the log.
     */
    String message() default "";
    
    /**
     * Log level to use for logging.
     */
    LogLevel level() default LogLevel.INFO;
    
    /**
     * Whether to log method arguments.
     */
    boolean logArgs() default true;
    
    /**
     * Whether to log method return value.
     */
    boolean logReturn() default true;
    
    /**
     * Whether to log execution time.
     */
    boolean logExecutionTime() default true;
    
    /**
     * Whether to log exceptions thrown by the method.
     */
    boolean logException() default true;
    
    /**
     * Available log levels.
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
