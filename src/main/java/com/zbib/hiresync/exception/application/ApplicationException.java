package com.zbib.hiresync.exception.application;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all job application-related exceptions
 */
public abstract class ApplicationException extends BaseException {
    protected ApplicationException(String message, HttpStatus status) {
        super(message, status);
    }
} 