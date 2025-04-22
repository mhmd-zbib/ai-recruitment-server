package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

/**
 * Exception thrown when a requested resource cannot be found
 */
public class ResourceNotFoundException extends BaseException {
    
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " not found with ID: " + id, HttpStatus.NOT_FOUND);
    }
} 