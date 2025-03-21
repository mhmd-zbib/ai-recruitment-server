package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Responsible for formatting log messages and context
 */
@Component
public class LogFormatter {

    /**
     * Format success response for logging
     */
    public void formatSuccessResponse(Object result, long executionTime, ObjectMapper jacksonObjectMapper) {
        // Add execution time to context
        ThreadContext.put("executionTime", String.valueOf(executionTime));

        // Serialize response
        try {
            String jsonResponse = result != null ? jacksonObjectMapper.writeValueAsString(result) : "{}";
            ThreadContext.put("response", jsonResponse);
        } catch (Exception e) {
            ThreadContext.put("response", "{\"error\":\"Could not serialize response\"}");
        }
    }

    /**
     * Format client error for logging
     */
    public void formatClientError(Throwable throwable, String message, long executionTime, ObjectMapper jacksonObjectMapper) {
        ThreadContext.put("executionTime", String.valueOf(executionTime));

        try {
            // Create basic error info without stack trace
            ClientErrorInfo errorInfo = new ClientErrorInfo(
                    throwable.getClass().getName(),
                    throwable.getMessage()
            );

            // Serialize error info to JSON
            String errorJson = jacksonObjectMapper.writeValueAsString(errorInfo);
            ThreadContext.put("errorInfo", errorJson);

            // Basic error response for the response field
            ThreadContext.put("response", "{\"error\": \"" + escapeSensitiveInfo(throwable.getMessage()) + "\"}");

        } catch (Exception e) {
            // Fallback if JSON serialization fails
            ThreadContext.put("errorInfo", "{\"error\": \"Error serializing exception details\"}");
            ThreadContext.put("response", "{\"error\": \"Client error\"}");
        }
    }

    /**
     * Format server error for logging
     */
    public void formatServerError(Throwable throwable, String message, long executionTime, ObjectMapper jacksonObjectMapper) {
        ThreadContext.put("executionTime", String.valueOf(executionTime));

        try {
            // Get stack trace as string
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            // Create structured error info with stack trace
            ServerErrorInfo errorInfo = new ServerErrorInfo(
                    throwable.getClass().getName(),
                    throwable.getMessage(),
                    stackTrace,
                    throwable.getCause() != null ? throwable.getCause().getMessage() : null
            );

            // Serialize error info to JSON
            String errorJson = jacksonObjectMapper.writeValueAsString(errorInfo);
            ThreadContext.put("errorInfo", errorJson);

            // Basic error response for the response field
            ThreadContext.put("response", "{\"error\": \"" + escapeSensitiveInfo(throwable.getMessage()) + "\"}");

        } catch (Exception e) {
            // Fallback if JSON serialization fails
            ThreadContext.put("errorInfo", "{\"error\": \"Error serializing exception details\"}");
            ThreadContext.put("response", "{\"error\": \"Internal server error\"}");
        }
    }

    /**
     * Prevents sensitive information from being logged
     */
    private String escapeSensitiveInfo(String message) {
        if (message == null) return "null";

        // Replace potential sensitive patterns like passwords, tokens, etc.
        return message
                .replaceAll("(?i)password\\s*[=:]\\s*[^,;\\s]+", "password=*****")
                .replaceAll("(?i)token\\s*[=:]\\s*[^,;\\s]+", "token=*****")
                .replaceAll("(?i)secret\\s*[=:]\\s*[^,;\\s]+", "secret=*****");
    }

    /**
     * Helper class for structured client error information (4xx)
     */
    private static class ClientErrorInfo {
        private final String exceptionType;
        private final String message;

        public ClientErrorInfo(String exceptionType, String message) {
            this.exceptionType = exceptionType;
            this.message = message;
        }

        // Getters needed for Jackson serialization
        public String getExceptionType() {
            return exceptionType;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Helper class for structured server error information (5xx)
     */
    private static class ServerErrorInfo {
        private final String exceptionType;
        private final String message;
        private final String stackTrace;
        private final String cause;

        public ServerErrorInfo(String exceptionType, String message, String stackTrace, String cause) {
            this.exceptionType = exceptionType;
            this.message = message;
            this.stackTrace = stackTrace;
            this.cause = cause;
        }

        // Getters needed for Jackson serialization
        public String getExceptionType() {
            return exceptionType;
        }

        public String getMessage() {
            return message;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public String getCause() {
            return cause;
        }
    }
}