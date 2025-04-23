package com.zbib.hiresync.exception.application;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user tries to create an application that already exists
 */
public class ApplicationAlreadyExistException extends ApplicationException {
    
    private static final String DEFAULT_MESSAGE = "You have already applied for this job";
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.CONFLICT;
    
    public ApplicationAlreadyExistException() {
        super(DEFAULT_MESSAGE, DEFAULT_STATUS);
    }
    
    public ApplicationAlreadyExistException(String email) {
        super("Application with email " + email + " already exists for this job post", DEFAULT_STATUS);
    }
}