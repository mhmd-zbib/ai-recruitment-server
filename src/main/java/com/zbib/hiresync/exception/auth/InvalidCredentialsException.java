package com.zbib.hiresync.exception.auth;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication credentials are invalid
 */
public class InvalidCredentialsException extends AuthException {
    
    private static final String DEFAULT_MESSAGE = "Invalid username or password";
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.UNAUTHORIZED;
    
    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE, DEFAULT_STATUS);
    }
    
    public InvalidCredentialsException(String details) {
        super(DEFAULT_MESSAGE + ": " + details, DEFAULT_STATUS);
    }
} 