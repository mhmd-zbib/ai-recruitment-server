package com.zbib.hiresync.exception.application;

import com.zbib.hiresync.enums.ApplicationStatus;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an application is in an invalid state for an operation
 */
public class InvalidApplicationStateException extends ApplicationException {
    
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.BAD_REQUEST;
    
    public InvalidApplicationStateException(String message) {
        super(message, DEFAULT_STATUS);
    }
    
    // Common validation failures
    public static InvalidApplicationStateException missingName() {
        return new InvalidApplicationStateException("Applicant name is required");
    }
    
    public static InvalidApplicationStateException missingEmail() {
        return new InvalidApplicationStateException("Applicant email is required");
    }
    
    public static InvalidApplicationStateException missingJob() {
        return new InvalidApplicationStateException("Job must be specified");
    }
    
    public static InvalidApplicationStateException terminalState(ApplicationStatus status) {
        return new InvalidApplicationStateException("Cannot change status from terminal state " + status);
    }
    
    public static InvalidApplicationStateException missingContactInfo() {
        return new InvalidApplicationStateException("Cannot schedule interview without contact information");
    }
} 