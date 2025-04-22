package com.zbib.hiresync.exception.jobpost;

import com.zbib.hiresync.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all job post-related exceptions
 */
public abstract class JobPostException extends BaseException {
    protected JobPostException(String message, HttpStatus status) {
        super(message, status);
    }
} 