package com.zbib.hiresync.exception.auth;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user provides invalid credentials during authentication
 */
public class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException() {
        super("Invalid username or password", HttpStatus.UNAUTHORIZED);
    }
    
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
} 