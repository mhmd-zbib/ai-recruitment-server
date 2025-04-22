package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a duplicate resource is detected
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT);
    }
} 