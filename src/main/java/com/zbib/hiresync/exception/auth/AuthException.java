package com.zbib.hiresync.exception.auth;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all authentication-related exceptions
 */
public abstract class AuthException extends BaseException {
    protected AuthException(String message, HttpStatus status) {
        super(message, status);
    }
} 