package com.zbib.hiresync.logging;

import java.util.Map;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbib.hiresync.exceptions.AppException;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

  private static final Logger log = LogManager.getLogger(LoggingAspect.class);

  private final ObjectMapper jacksonObjectMapper;
  private final HttpServletRequest request;
  private final LogContextBuilder logContextBuilder;
  private final LogFormatter logFormatter;

  @Pointcut("within(com.zbib.hiresync.controller..*)")
  public void loggableMethods() {}

  @Around("loggableMethods()")
  public Object logAroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();

    Map<String, String> logContext = logContextBuilder.buildInitialContext(joinPoint, request);
    logContext.forEach(ThreadContext::put);

    try {
      Object result = joinPoint.proceed();
      logSuccessResponse(result, startTime);
      return result;
    } catch (Exception ex) {
      logError(ex, startTime, logContext.get("message"));
      throw ex;
    } finally {
      ThreadContext.clearMap();
    }
  }

  private void logSuccessResponse(Object result, long startTime) {
    long executionTime = System.currentTimeMillis() - startTime;
    logFormatter.formatSuccessResponse(result, executionTime, jacksonObjectMapper);
    log.info(ThreadContext.get("message"));
  }

  private void logError(Exception ex, long startTime, String message) {
    long executionTime = System.currentTimeMillis() - startTime;
    HttpStatus status = determineHttpStatus(ex);

    String errorMessage =
        String.format("%s - %s: %s", message, ex.getClass().getName(), ex.getMessage());

    if (status.is4xxClientError()) {
      logFormatter.formatClientError(ex, message, executionTime, jacksonObjectMapper);
      log.warn(errorMessage);
    } else {
      logFormatter.formatServerError(ex, message, executionTime, jacksonObjectMapper);
      log.error(errorMessage, ex);
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
