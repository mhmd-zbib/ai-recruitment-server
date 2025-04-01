package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

/**
 * Component for formatting and structuring log messages. Handles different types of log scenarios
 * including success responses and errors.
 */
@Component
public class LogFormatter {

  /**
   * Formats successful operation responses for logging.
   *
   * @param result the operation result to be logged
   * @param executionTime the time taken to execute the operation
   * @param jacksonObjectMapper the ObjectMapper to serialize responses
   */
  public void formatSuccessResponse(
      Object result, long executionTime, ObjectMapper jacksonObjectMapper) {
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
   * Formats client error information for logging.
   *
   * @param throwable the exception that occurred
   * @param message additional error message
   * @param executionTime the time taken before the error occurred
   * @param jacksonObjectMapper the ObjectMapper to serialize error details
   */
  public void formatClientError(
      Throwable throwable, String message, long executionTime, ObjectMapper jacksonObjectMapper) {
    ThreadContext.put("executionTime", String.valueOf(executionTime));

    try {
      // Create basic error info without stack trace
      ClientErrorInfo errorInfo =
          new ClientErrorInfo(throwable.getClass().getName(), throwable.getMessage());

      // Serialize error info to JSON
      String errorJson = jacksonObjectMapper.writeValueAsString(errorInfo);
      ThreadContext.put("errorInfo", errorJson);

      // Basic error response for the response field
      ThreadContext.put(
          "response", "{\"error\": \"" + escapeSensitiveInfo(throwable.getMessage()) + "\"}");

    } catch (Exception e) {
      // Fallback if serialization fails
      ThreadContext.put("errorInfo", "{\"error\": \"Error serializing exception details\"}");
      ThreadContext.put("response", "{\"error\": \"Client error\"}");
    }
  }

  /**
   * Formats server error information for logging with full stack trace.
   *
   * @param throwable the exception that occurred
   * @param message additional error message
   * @param executionTime the time taken before the error occurred
   * @param jacksonObjectMapper the ObjectMapper to serialize error details
   */
  public void formatServerError(
      Throwable throwable, String message, long executionTime, ObjectMapper jacksonObjectMapper) {
    ThreadContext.put("executionTime", String.valueOf(executionTime));

    try {
      // Get stack trace as string
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      throwable.printStackTrace(pw);
      String stackTrace = sw.toString();

      // Create structured error info with stack trace
      ServerErrorInfo errorInfo =
          new ServerErrorInfo(
              throwable.getClass().getName(),
              throwable.getMessage(),
              stackTrace,
              throwable.getCause() != null ? throwable.getCause().getMessage() : null);

      // Serialize error info to JSON
      String errorJson = jacksonObjectMapper.writeValueAsString(errorInfo);
      ThreadContext.put("errorInfo", errorJson);

      // Basic error response for the response field
      ThreadContext.put(
          "response", "{\"error\": \"" + escapeSensitiveInfo(throwable.getMessage()) + "\"}");

    } catch (Exception e) {
      // Fallback if JSON serialization fails
      ThreadContext.put("errorInfo", "{\"error\": \"Error serializing exception details\"}");
      ThreadContext.put("response", "{\"error\": \"Internal server error\"}");
    }
  }

  /**
   * Removes sensitive information from error messages.
   *
   * @param message the original error message
   * @return the error message with sensitive information masked
   */
  private String escapeSensitiveInfo(String message) {
    if (message == null) {
      return "null";
    }

    // Replace potential sensitive patterns like passwords, tokens, etc.
    return message
        .replaceAll("(?i)password\\s*[=:]\\s*[^,;\\s]+", "password=*****")
        .replaceAll("(?i)token\\s*[=:]\\s*[^,;\\s]+", "token=*****")
        .replaceAll("(?i)secret\\s*[=:]\\s*[^,;\\s]+", "secret=*****");
  }

  /** Record for storing client error information. */
  private record ClientErrorInfo(String exceptionType, String message) {}

  /** Record for storing server error information with stack trace. */
  private record ServerErrorInfo(
      String exceptionType, String message, String stackTrace, String cause) {}
}
