package com.zbib.hiresync.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response format for the application.
 * Follows industry standard with status, message, and timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    /**
     * HTTP status code
     */
    private int status;

    /**
     * Error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
}
