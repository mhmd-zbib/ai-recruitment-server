package com.zbib.hiresync.exception.jobpost;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when trying to apply to a job post that is not active
 */
public class JobPostNotActiveException extends JobPostException {
    public JobPostNotActiveException() {
        super("Job post not found or no longer active", HttpStatus.NOT_FOUND);
    }
    
    public JobPostNotActiveException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
} 