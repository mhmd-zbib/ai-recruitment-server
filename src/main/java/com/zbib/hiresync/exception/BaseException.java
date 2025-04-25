package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all application exceptions
 */
public abstract class BaseException extends RuntimeException {
    private final HttpStatus status;
    
    protected BaseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    protected BaseException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
} 