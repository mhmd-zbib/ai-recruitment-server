package com.zbib.hiresync.exception;

/**
 * Exception thrown when there is an error with the AI service.
 */
public class AIServiceException extends RuntimeException {
    
    /**
     * Constructs a new AIServiceException with the specified detail message.
     *
     * @param message the detail message
     */
    public AIServiceException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new AIServiceException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
