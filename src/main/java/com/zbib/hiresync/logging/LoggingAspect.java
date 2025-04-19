package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hiresync.logging.method-logging-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAspect {
    
    private final MaskingUtils maskingUtils;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.zbib.hiresync.logging.LoggableService)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LoggableService annotation = method.getAnnotation(LoggableService.class);
        
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        String message = annotation.message().isEmpty() 
                ? "Executing " + methodName 
                : annotation.message();
        
        // Capture method arguments if enabled
        if (annotation.logArguments()) {
            logMethodArguments(joinPoint, signature, annotation.sensitiveFields());
        }
        
        // Start timing and execute method
        StopWatch stopWatch = new StopWatch();
        stopWatch.start(methodName);
        
        try {
            // Log method entry
            log(annotation.level(), "{} - Started", message);
            
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Stop timing
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            
            // Log method exit
            log(annotation.level(), "{} - Completed in {}ms", message, executionTime);
            
            return result;
        } catch (Throwable ex) {
            // Stop timing in case of exception
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            long executionTime = stopWatch.getTotalTimeMillis();
            
            // Log exception with method context
            log(LogLevel.ERROR, "{} - Failed after {}ms: {}", 
                    message, executionTime, ex.getMessage());
            
            throw ex;
        }
    }
    
    private void logMethodArguments(ProceedingJoinPoint joinPoint, MethodSignature signature, 
                                    String[] sensitiveFields) {
        try {
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            if (paramNames.length == 0 || args.length == 0) {
                return;
            }
            
            Map<String, Object> argsMap = IntStream.range(0, Math.min(paramNames.length, args.length))
                    .boxed()
                    .filter(i -> args[i] != null)
                    .collect(Collectors.toMap(
                            i -> paramNames[i],
                            i -> args[i],
                            (k1, k2) -> k2
                    ));
            
            if (!argsMap.isEmpty()) {
                // Mask sensitive values before logging
                String maskedArgs = maskingUtils.maskObject(argsMap);
                
                // Custom masking for explicitly specified sensitive fields
                if (sensitiveFields.length > 0) {
                    // Convert to Map to apply additional masking
                    Map<String, Object> argMap = new HashMap<>();
                    try {
                        // Parse the JSON representation
                        argMap = objectMapper.readValue(maskedArgs, Map.class);
                        
                        // Apply additional masking for specified sensitive fields
                        for (String field : sensitiveFields) {
                            maskSensitiveField(argMap, field);
                        }
                        
                        // Convert back to string
                        maskedArgs = objectMapper.writeValueAsString(argMap);
                    } catch (Exception e) {
                        // Fallback to original masked string if JSON parsing fails
                    }
                }
                
                log(LogLevel.DEBUG, "Method arguments: {}", maskedArgs);
            }
        } catch (Exception e) {
            // In case of any error during argument logging, don't let it affect the main method execution
            log(LogLevel.WARN, "Failed to log method arguments: {}", e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void maskSensitiveField(Map<String, Object> map, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        
        if (parts.length == 1) {
            // Direct field in the map
            if (map.containsKey(parts[0])) {
                map.put(parts[0], "********");
            }
            return;
        }
        
        // Handle nested fields
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            Object value = current.get(parts[i]);
            if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else {
                return; // Path doesn't exist, nothing to mask
            }
        }
        
        // Mask the leaf field
        String leafKey = parts[parts.length - 1];
        if (current.containsKey(leafKey)) {
            current.put(leafKey, "********");
        }
    }
    
    private void log(LogLevel level, String format, Object... args) {
        switch (level) {
            case DEBUG:
                log.debug(format, args);
                break;
            case INFO:
                log.info(format, args);
                break;
            case WARN:
                log.warn(format, args);
                break;
            case ERROR:
                log.error(format, args);
                break;
        }
    }
}