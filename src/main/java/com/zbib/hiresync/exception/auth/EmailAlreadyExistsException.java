package com.zbib.hiresync.exception.auth;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when trying to register with an email that already exists
 */
public class EmailAlreadyExistsException extends AuthException {
    public EmailAlreadyExistsException() {
        super("Email is already in use", HttpStatus.CONFLICT);
    }
    
    public EmailAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
} 