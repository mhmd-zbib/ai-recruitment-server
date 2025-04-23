package com.zbib.hiresync.exception.job;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a job is in an invalid state for an operation
 */
public class InvalidJobStateException extends JobException {
    
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.BAD_REQUEST;
    
    public InvalidJobStateException(String message) {
        super(message, DEFAULT_STATUS);
    }
    
    public static InvalidJobStateException missingTitle() {
        return new InvalidJobStateException("Job title is required");
    }
    
    public static InvalidJobStateException missingDescription() {
        return new InvalidJobStateException("Job description is required");
    }
    
    public static InvalidJobStateException missingRequirements() {
        return new InvalidJobStateException("Job requirements are required");
    }
    
    public static InvalidJobStateException missingCompanyName() {
        return new InvalidJobStateException("Company name is required");
    }
    
    public static InvalidJobStateException missingWorkplaceType() {
        return new InvalidJobStateException("Workplace type is required");
    }
    
    public static InvalidJobStateException missingEmploymentType() {
        return new InvalidJobStateException("Employment type is required");
    }
    
    public static InvalidJobStateException invalidSalaryRange() {
        return new InvalidJobStateException("Both minimum and maximum salary must be specified if one is provided");
    }
    
    public static InvalidJobStateException minGreaterThanMax() {
        return new InvalidJobStateException("Minimum salary cannot be greater than maximum salary");
    }
    
    public static InvalidJobStateException missingVisibleUntil() {
        return new InvalidJobStateException("Visibility end date is required and must be in the future");
    }
    
    public static InvalidJobStateException cannotActivateIncomplete(String details) {
        return new InvalidJobStateException("Cannot activate incomplete job: " + details);
    }
} 