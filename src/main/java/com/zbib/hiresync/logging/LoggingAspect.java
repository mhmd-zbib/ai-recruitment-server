package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbib.hiresync.exceptions.AppException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Needed for comprehensive logging in AOP
public class LoggingAspect {

  private static final Logger LOG = LogManager.getLogger(LoggingAspect.class);

  private final ObjectMapper jacksonObjectMapper;
  private final HttpServletRequest request;
  private final LogContextBuilder logContextBuilder;
  private final LogFormatter logFormatter;

  /**
   * Pointcut definition for controller methods that should be logged. This method is intentionally
   * empty as it's used only as a pointcut designator.
   */
  @Pointcut("within(com.zbib.hiresync.controller..*)")
  public void loggableMethods() {
    // This method is intentionally empty - used only for pointcut definition
  }

  @Around("loggableMethods()")
  public Object logAroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();
    Map<String, String> logContext = null;

    try {
      logContext = logContextBuilder.buildInitialContext(joinPoint, request);
      logContext.forEach(ThreadContext::put);

      try {
        // This call can throw Throwable which we need to handle separately
        Object result = joinPoint.proceed();
        logSuccessResponse(result, startTime);
        return result;
      } catch (Error error) {
        // Handle JVM errors like OutOfMemoryError, StackOverflowError separately
        logError(
            new RuntimeException("System error: " + error.getMessage(), error),
            startTime,
            logContext != null ? logContext.get("message") : "Critical system error");
        throw error; // Rethrow as-is since JVM errors should not be caught and handled
      }
    } catch (ResponseStatusException ex) {
      logError(
          ex, startTime, logContext != null ? logContext.get("message") : "API request failed");
      throw ex;
    } catch (AppException ex) {
      logError(ex, startTime, logContext != null ? logContext.get("message") : "Application error");
      throw ex;
    } catch (IllegalArgumentException | IllegalStateException ex) {
      logError(
          ex,
          startTime,
          logContext != null ? logContext.get("message") : "Request validation error");
      throw ex;
    } catch (UnsupportedOperationException ex) {
      logError(
          ex, startTime, logContext != null ? logContext.get("message") : "Unsupported operation");
      throw ex;
    } catch (java.io.IOException ex) {
      logError(ex, startTime, logContext != null ? logContext.get("message") : "IO error");
      throw ex;
    } catch (ReflectiveOperationException ex) {
      logError(ex, startTime, logContext != null ? logContext.get("message") : "Reflection error");
      throw ex;
    } catch (InterruptedException ex) {
      logError(
          ex, startTime, logContext != null ? logContext.get("message") : "Thread interrupted");
      // Reset the interrupted status
      Thread.currentThread().interrupt();
      throw ex;
    } catch (SecurityException ex) {
      logError(
          ex, startTime, logContext != null ? logContext.get("message") : "Security violation");
      throw ex;
    } catch (RuntimeException ex) {
      // Handle any other runtime exceptions
      logError(
          ex,
          startTime,
          logContext != null ? logContext.get("message") : "Unexpected runtime error");
      throw ex;
    } catch (Exception ex) {
      // Handle all other checked exceptions
      logError(ex, startTime, logContext != null ? logContext.get("message") : "Unexpected error");
      throw ex;
    } finally {
      ThreadContext.clearMap();
    }
  }

  private void logSuccessResponse(Object result, long startTime) {
    long executionTime = System.currentTimeMillis() - startTime;
    logFormatter.formatSuccessResponse(result, executionTime, jacksonObjectMapper);
    LOG.info(ThreadContext.get("message"));
  }

  private void logError(Exception ex, long startTime, String message) {
    long executionTime = System.currentTimeMillis() - startTime;
    HttpStatus status = determineHttpStatus(ex);

    String errorMessage =
        String.format("%s - %s: %s", message, ex.getClass().getName(), ex.getMessage());

    if (status.is4xxClientError()) {
      logFormatter.formatClientError(ex, message, executionTime, jacksonObjectMapper);
      LOG.warn(errorMessage);
    } else {
      logFormatter.formatServerError(ex, message, executionTime, jacksonObjectMapper);
      LOG.error(errorMessage, ex);
    }
  }

  private HttpStatus determineHttpStatus(Exception ex) {
    if (ex instanceof ResponseStatusException) {
      return (HttpStatus) ((ResponseStatusException) ex).getStatusCode();
    } else if (ex instanceof AppException) {
      return ((AppException) ex).getStatus();
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
