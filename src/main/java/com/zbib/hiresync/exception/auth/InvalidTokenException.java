package com.zbib.hiresync.exception.auth;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an invalid token is provided during authentication
 */
public class InvalidTokenException extends AuthException {
    public InvalidTokenException() {
        super("Invalid token", HttpStatus.UNAUTHORIZED);
    }
    
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
} 