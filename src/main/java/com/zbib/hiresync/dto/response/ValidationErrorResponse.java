package com.zbib.hiresync.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Validation error response format.
 * Extends the standard error response with validation details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {
    /**
     * HTTP status code
     */
    private int status;

    /**
     * Error message
     */
    private String message;

    /**
     * List of validation errors
     */
    private List<Map<String, String>> details;

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
}
