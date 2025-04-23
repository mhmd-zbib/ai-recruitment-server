package com.zbib.hiresync.exception.job;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a job cannot be found
 */
public class JobNotFoundException extends JobException {
    
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.NOT_FOUND;
    private static final String DEFAULT_MESSAGE = "Job not found";
    
    public JobNotFoundException() {
        super(DEFAULT_MESSAGE, DEFAULT_STATUS);
    }
    
    public JobNotFoundException(String message) {
        super(message, DEFAULT_STATUS);
    }
} 