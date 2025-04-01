package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogContextBuilder {

  private final ObjectMapper jacksonObjectMapper;
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final String USER_AGENT = "User-Agent";

  public Map<String, String> buildInitialContext(
      ProceedingJoinPoint joinPoint, HttpServletRequest request) {
    Map<String, String> context = new HashMap<>();

    // Get correlation ID and request information
    String correlationId = getCorrelationId(request);
    String apiPath = request.getRequestURI();
    String httpMethod = request.getMethod();
    String message = getLoggableMessage(joinPoint);

    // Extract client information
    String clientIp = getClientIp(request);
    String userAgent = request.getHeader(USER_AGENT);

    // Process request body
    String jsonRequest = formatRequestBody(joinPoint.getArgs());

    // Populate context map
    context.put("message", message);
    context.put("request", jsonRequest);
    context.put("response", "");
    context.put("apiPath", apiPath);
    context.put("httpMethod", httpMethod);
    context.put("correlationId", correlationId);
    context.put("clientIp", clientIp);
    context.put("userAgent", userAgent);

    return context;
  }

  private String formatRequestBody(Object[] args) {
    try {
      return args.length > 0 ? jacksonObjectMapper.writeValueAsString(args[0]) : "{}";
    } catch (Exception e) {
      return "{\"error\":\"Could not serialize request\"}";
    }
  }

  private String getCorrelationId(HttpServletRequest request) {
    String correlationId = request.getHeader("X-Correlation-Id");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }
    return correlationId;
  }

  private String getClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader(X_FORWARDED_FOR);
    if (forwardedFor != null && !forwardedFor.isEmpty()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String getLoggableMessage(ProceedingJoinPoint joinPoint) {
    try {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      Method method = signature.getMethod();

      Loggable loggable = method.getAnnotation(Loggable.class);

      if (loggable == null) {
        loggable = method.getDeclaringClass().getAnnotation(Loggable.class);
      }
      if (loggable != null) {
        return loggable.message();
      }

      String className = method.getDeclaringClass().getSimpleName();
      String methodName = method.getName();
      return String.format("Executing %s.%s", className, methodName);
    } catch (Exception e) {
      return "Method Execution";
    }
  }
}
