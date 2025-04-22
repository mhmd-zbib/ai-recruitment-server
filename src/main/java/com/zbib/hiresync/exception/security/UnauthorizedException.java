package com.zbib.hiresync.exception.security;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is not authorized to access a resource
 */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException() {
        super("You are not authorized to access this resource", HttpStatus.FORBIDDEN);
    }
    
    public UnauthorizedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
} 