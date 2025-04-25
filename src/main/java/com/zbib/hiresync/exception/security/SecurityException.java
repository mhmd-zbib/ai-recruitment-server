package com.zbib.hiresync.exception.security;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Base class for all security-related exceptions
 */
public abstract class SecurityException extends BaseException {
    
    protected SecurityException(String message, HttpStatus status) {
        super(message, status);
    }
} 