package com.zbib.hiresync.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

/**
 * Global exception handler for the application.
 * Handles all exceptions and provides consistent error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles AppException and its subclasses.
     *
     * @param ex the exception to handle
     * @param request the current request
     * @return a ResponseEntity with the error details
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        // Log the exception with structured information
        logger.error("Application exception: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = buildErrorResponse(ex, request);
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    /**
     * Handles all other exceptions not specifically handled elsewhere.
     *
     * @param ex the exception to handle
     * @param request the current request
     * @return a ResponseEntity with the error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        // Log the exception with structured information
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .details(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .requestId(MDC.get("requestId"))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Builds an ErrorResponse from an AppException.
     *
     * @param ex the AppException
     * @param request the current request
     * @return an ErrorResponse
     */
    private ErrorResponse buildErrorResponse(AppException ex, HttpServletRequest request) {
        return ErrorResponse.builder()
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .requestId(MDC.get("requestId"))
                .build();
    }
}