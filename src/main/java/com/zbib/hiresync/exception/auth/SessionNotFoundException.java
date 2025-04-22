package com.zbib.hiresync.exception.auth;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user session cannot be found
 */
public class SessionNotFoundException extends AuthException {
    public SessionNotFoundException() {
        super("Session not found", HttpStatus.NOT_FOUND);
    }
    
    public SessionNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
} 