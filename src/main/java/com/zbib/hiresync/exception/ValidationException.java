package com.zbib.hiresync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
public class ValidationException extends AppException {
    
    private final Map<String, String> errors;
    
    private ValidationException(HttpStatus status, String userMessage, String logMessage, Map<String, String> errors) {
        super(status, userMessage, logMessage);
        this.errors = errors;
    }
    
    public static ValidationException invalidInput(Map<String, String> errors) {
        return new ValidationException(BAD_REQUEST,
                "Invalid input data",
                "Validation failed: " + errors,
                errors);
    }
    
    public static ValidationException missingRequiredFields(Map<String, String> errors) {
        return new ValidationException(BAD_REQUEST,
                "Missing required fields",
                "Missing required fields: " + errors,
                errors);
    }
}
