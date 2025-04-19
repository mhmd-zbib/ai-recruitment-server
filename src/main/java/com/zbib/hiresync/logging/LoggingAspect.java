package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Aspect for logging method execution with detailed information.
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hiresync.logging.method-logging-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAspect {
    private static final Logger logger = LogManager.getLogger(LoggingAspect.class);
    
    private final MaskingUtils maskingUtils;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(com.zbib.hiresync.logging.LoggableService)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        LoggableService annotation = signature.getMethod().getAnnotation(LoggableService.class);
        
        // Create log message
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        String message = annotation.message().isEmpty() ? "Executing " + methodName : annotation.message();
        
        // Add context for structured logging
        ThreadContext.put(ContextKeys.CLASS, signature.getDeclaringTypeName());
        ThreadContext.put(ContextKeys.METHOD, signature.getName());
        
        // Log arguments if enabled
        if (annotation.logArguments()) {
            logArguments(joinPoint, signature, annotation.sensitiveFields());
        }
        
        // Execute method and time it
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            // Log method entry - this shows in both terminal and file logs
            log(annotation.level(), "{} - Started", message);
            
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Log method success
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            ThreadContext.put(ContextKeys.EXECUTION_TIME, String.valueOf(executionTime));
            log(annotation.level(), "{} - Completed in {}ms", message, executionTime);
            
            return result;
        } catch (Throwable ex) {
            // Log method failure
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            
            // Add error context
            long executionTime = stopWatch.getTotalTimeMillis();
            ThreadContext.put(ContextKeys.EXCEPTION, ex.getClass().getName());
            ThreadContext.put(ContextKeys.ERROR_MESSAGE, ex.getMessage());
            ThreadContext.put(ContextKeys.EXECUTION_TIME, String.valueOf(executionTime));
            
            log(LogLevel.ERROR, "{} - Failed after {}ms: {}", message, executionTime, ex.getMessage());
            throw ex;
        } finally {
            // Clean up context
            ThreadContext.remove(ContextKeys.CLASS);
            ThreadContext.remove(ContextKeys.METHOD);
            ThreadContext.remove(ContextKeys.EXECUTION_TIME);
            ThreadContext.remove(ContextKeys.EXCEPTION);
            ThreadContext.remove(ContextKeys.ERROR_MESSAGE);
        }
    }
    
    private void logArguments(ProceedingJoinPoint joinPoint, MethodSignature signature, String[] sensitiveFields) {
        try {
            Map<String, Object> argsMap = extractArgsMap(joinPoint, signature);
            
            if (!argsMap.isEmpty()) {
                String maskedArgs = maskingUtils.maskObject(argsMap);
                log(LogLevel.DEBUG, "Method arguments: {}", maskedArgs);
            }
        } catch (Exception e) {
            log(LogLevel.WARN, "Failed to log arguments: {}", e.getMessage());
        }
    }
    
    private Map<String, Object> extractArgsMap(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        if (paramNames.length == 0 || args.length == 0) {
            return Map.of();
        }
        
        return IntStream.range(0, Math.min(paramNames.length, args.length))
                .boxed()
                .filter(i -> args[i] != null)
                .collect(Collectors.toMap(
                        i -> paramNames[i],
                        i -> args[i],
                        (k1, k2) -> k2
                ));
    }
    
    private void log(LogLevel level, String format, Object... args) {
        switch (level) {
            case DEBUG -> logger.debug(format, args);
            case INFO -> logger.info(format, args);
            case WARN -> logger.warn(format, args);
            case ERROR -> logger.error(format, args);
        }
    }
}