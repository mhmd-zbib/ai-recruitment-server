package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

@Component
public class LogFormatter {

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

  private String escapeSensitiveInfo(String message) {
    if (message == null) return "null";

    // Replace potential sensitive patterns like passwords, tokens, etc.
    return message
        .replaceAll("(?i)password\\s*[=:]\\s*[^,;\\s]+", "password=*****")
        .replaceAll("(?i)token\\s*[=:]\\s*[^,;\\s]+", "token=*****")
        .replaceAll("(?i)secret\\s*[=:]\\s*[^,;\\s]+", "secret=*****");
  }

  private record ClientErrorInfo(String exceptionType, String message) {}

  private record ServerErrorInfo(
      String exceptionType, String message, String stackTrace, String cause) {}
}
