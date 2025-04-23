package com.zbib.hiresync.exception.job;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all job-related exceptions
 */
public abstract class JobException extends BaseException {
    protected JobException(String message, HttpStatus status) {
        super(message, status);
    }
} 