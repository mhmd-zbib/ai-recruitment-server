package com.zbib.hiresync.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for all application exceptions.
 * All custom exceptions in the application should extend this class.
 */
@Getter
public class AppException extends RuntimeException {
    
    private final HttpStatus status;
    private final String message;
    private final String details;
    
    /**
     * Constructs a new AppException with the specified status, message, and details.
     *
     * @param status the HTTP status code associated with this exception
     * @param message the error message
     * @param details additional details about the error
     */
    public AppException(HttpStatus status, String message, String details) {
        super(message);
        this.status = status;
        this.message = message;
        this.details = details;
    }
    
    /**
     * Constructs a new AppException with the specified status and message.
     *
     * @param status the HTTP status code associated with this exception
     * @param message the error message
     */
    public AppException(HttpStatus status, String message) {
        this(status, message, null);
    }
}