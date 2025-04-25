package com.zbib.hiresync.exception.application;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class ApplicationNotFoundException extends ApplicationException {
    public ApplicationNotFoundException(UUID id) {
        super("Application not found with ID: " + id, HttpStatus.NOT_FOUND);
    }
} 