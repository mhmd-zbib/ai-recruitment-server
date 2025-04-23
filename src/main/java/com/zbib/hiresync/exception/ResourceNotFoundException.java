package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

/**
 * Exception thrown when a requested resource could not be found
 */
public class ResourceNotFoundException extends BaseException {
    
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.NOT_FOUND;
    
    public ResourceNotFoundException(String message) {
        super(message, DEFAULT_STATUS);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, DEFAULT_STATUS);
    }
    
    public ResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " not found with ID: " + id, DEFAULT_STATUS);
    }
    
    // Factory methods for common cases
    public static ResourceNotFoundException jobPost(UUID id) {
        return new ResourceNotFoundException("Job post not found with ID: " + id);
    }
    
    public static ResourceNotFoundException application(UUID id) {
        return new ResourceNotFoundException("Application not found with ID: " + id);
    }
    
    public static ResourceNotFoundException user(String username) {
        return new ResourceNotFoundException("User not found with email: " + username);
    }
} 