package com.zbib.hiresync.exception.security;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for
 */
public class UnauthorizedException extends SecurityException {
    
    private static final String DEFAULT_MESSAGE = "You are not authorized to perform this action";
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.FORBIDDEN;
    
    public UnauthorizedException() {
        super(DEFAULT_MESSAGE, DEFAULT_STATUS);
    }
    
    public UnauthorizedException(String message) {
        super(message, DEFAULT_STATUS);
    }
    
    // Common authorization failures
    public static UnauthorizedException viewApplications() {
        return new UnauthorizedException("You are not authorized to view applications");
    }
    
    public static UnauthorizedException viewStatistics() {
        return new UnauthorizedException("You are not authorized to view application statistics");
    }
    
    public static UnauthorizedException modifyJobPost() {
        return new UnauthorizedException("You are not authorized to modify this job post");
    }
    
    public static UnauthorizedException deleteJobPost() {
        return new UnauthorizedException("You are not authorized to delete this job post");
    }
    
    public static UnauthorizedException deleteJobPostWithApplications() {
        return new UnauthorizedException("Cannot delete job post with active applications");
    }
    
    public static UnauthorizedException updateApplication() {
        return new UnauthorizedException("You are not authorized to update this application");
    }
    
    public static UnauthorizedException deleteApplication() {
        return new UnauthorizedException("You are not authorized to delete this application");
    }
    
    public static UnauthorizedException viewJobPostApplications() {
        return new UnauthorizedException("You are not authorized to view applications for this job post");
    }
} 