package com.zbib.hiresync.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LogManager.getLogger(LoggingAspect.class);

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {
        // Pointcut for all REST controllers
    }

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void servicePointcut() {
        // Pointcut for all service classes
    }

    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryPointcut() {
        // Pointcut for all repository classes
    }

    @Around("controllerPointcut() || servicePointcut() || repositoryPointcut()")
    public Object logAroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = getCorrelationId();
        ThreadContext.put("correlation_id", correlationId);
        ThreadContext.put("requestId", correlationId);

        // Get method details
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        // Add method parameters to thread context instead of embedding as JSON
        ThreadContext.put("event_type", "method_entry");
        ThreadContext.put("method", methodName);

        // Log parameters directly
        try {
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();

            for (int i = 0; i < paramNames.length; i++) {
                // Skip logging request/response objects and potentially sensitive data
                if (!isHttpRequestOrResponse(paramValues[i]) && !isSensitiveData(paramNames[i])) {
                    // Add each parameter to MDC
                    if (paramValues[i] != null) {
                        ThreadContext.put("param_" + paramNames[i], paramValues[i].toString());
                    }
                }
            }

            logger.info("Method entry: {}", methodName);
        } catch (Exception e) {
            logger.warn("Failed to log method parameters: {}", e.getMessage());
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            // Execute the actual method
            result = joinPoint.proceed();

            // Clear previous MDC values
            clearMethodMdc();

            // Log the response
            ThreadContext.put("event_type", "method_exit");
            ThreadContext.put("method", methodName);
            ThreadContext.put("execution_time_ms", String.valueOf(System.currentTimeMillis() - startTime));

            logger.info("Method exit: {} completed in {}ms", methodName, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            // Clear previous MDC values
            clearMethodMdc();

            // Log errors
            ThreadContext.put("event_type", "method_error");
            ThreadContext.put("method", methodName);
            ThreadContext.put("execution_time_ms", String.valueOf(System.currentTimeMillis() - startTime));
            ThreadContext.put("error_type", e.getClass().getName());
            ThreadContext.put("error_message", e.getMessage());

            logger.error("Method error: {} failed", methodName, e);
            throw e;
        } finally {
            clearAllMdc();
        }
    }

    private void clearMethodMdc() {
        ThreadContext.remove("event_type");
        ThreadContext.remove("method");
        ThreadContext.remove("execution_time_ms");
        ThreadContext.remove("error_type");
        ThreadContext.remove("error_message");

        // Remove all param_ entries
        ThreadContext.getContext().entrySet().removeIf(entry -> entry.getKey().startsWith("param_"));
    }

    private void clearAllMdc() {
        ThreadContext.clearAll();
    }

    private String getCorrelationId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String correlationId = request.getHeader("X-Correlation-ID");
                if (correlationId != null && !correlationId.isEmpty()) {
                    return correlationId;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get correlation ID from request: {}", e.getMessage());
        }

        return UUID.randomUUID().toString();
    }

    private boolean isHttpRequestOrResponse(Object obj) {
        if (obj == null) return false;
        String className = obj.getClass().getName();
        return className.contains("HttpServletRequest") ||
                className.contains("HttpServletResponse") ||
                className.contains("MultipartFile");
    }

    private boolean isSensitiveData(String paramName) {
        // Add your rules for sensitive data
        return paramName != null && (
                paramName.toLowerCase().contains("password") ||
                        paramName.toLowerCase().contains("token") ||
                        paramName.toLowerCase().contains("secret") ||
                        paramName.toLowerCase().contains("credential")
        );
    }
}