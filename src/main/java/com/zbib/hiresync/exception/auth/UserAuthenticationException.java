package com.zbib.hiresync.exception.auth;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when there's an authentication error related to user credentials
 */
public class UserAuthenticationException extends AuthException {
    public UserAuthenticationException() {
        super("Authentication failed", HttpStatus.UNAUTHORIZED);
    }
    
    public UserAuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
} 