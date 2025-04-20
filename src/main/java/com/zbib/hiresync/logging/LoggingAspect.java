package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Aspect for logging method execution with detailed information.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Order(1) // Ensure this aspect runs first
@ConditionalOnProperty(name = "hiresync.logging.method-logging-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAspect {
    private static final Logger logger = LogManager.getLogger(LoggingAspect.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    // Track method calls within a single thread to avoid duplicate logging
    private static final ThreadLocal<Set<String>> LOGGED_METHODS = ThreadLocal.withInitial(ConcurrentHashMap::newKeySet);
    
    private final MaskingUtils maskingUtils;
    private final ObjectMapper objectMapper;
    
    /**
     * Define a pointcut for controller methods
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}
    
    /**
     * Define a pointcut for service methods
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}
    
    /**
     * Define a pointcut for methods annotated with LoggableService
     */
    @Pointcut("@annotation(com.zbib.hiresync.logging.LoggableService)")
    public void loggableMethods() {}
    
    /**
     * Main logging pointcut that avoids duplicate logging of the same logical operation
     */
    @Around("loggableMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        
        // Generate a unique ID for this method call including class/method and parameters hash
        String methodId = signature.getDeclaringTypeName() + "." + signature.getName() + 
                          "#" + joinPoint.getArgs().length;
        
        // Skip if we're already logging this method or one of its callers
        if (!LOGGED_METHODS.get().add(methodId)) {
            // Just proceed without logging to avoid duplication
            return joinPoint.proceed();
        }
        
        try {
            return doLogMethodExecution(joinPoint, signature);
        } finally {
            // Clean up after method execution
            LOGGED_METHODS.get().remove(methodId);
            if (LOGGED_METHODS.get().isEmpty()) {
                LOGGED_METHODS.remove();
            }
        }
    }
    
    /**
     * Actual logging implementation
     */
    private Object doLogMethodExecution(ProceedingJoinPoint joinPoint, MethodSignature signature) throws Throwable {
        LoggableService annotation = signature.getMethod().getAnnotation(LoggableService.class);
        
        // Create log message template
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        String messageTemplate = annotation.message().isEmpty() ? "Executing " + methodName : annotation.message();
        
        // Add context for structured logging
        ThreadContext.put(ContextKeys.CLASS, signature.getDeclaringTypeName());
        ThreadContext.put(ContextKeys.METHOD, signature.getName());
        
        // Extract arguments map for placeholder substitution
        Map<String, Object> argsMap = extractArgsMap(joinPoint, signature);
        
        // Process placeholders in the message
        String processedMessage = processPlaceholders(messageTemplate, argsMap);
        
        // Execute method and time it
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            // Execute the method without logging the start
            Object result = joinPoint.proceed();
            
            // Log method success only once at completion
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            ThreadContext.put(ContextKeys.EXECUTION_TIME, String.valueOf(executionTime));
            
            // Log concise message with execution time
            log(annotation.level(), "{} ({}ms)", processedMessage, executionTime);
            
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
            
            log(LogLevel.ERROR, "{} failed after {}ms: {}", processedMessage, executionTime, ex.getMessage());
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
    
    /**
     * Processes placeholders in the message template using the argument values
     */
    private String processPlaceholders(String template, Map<String, Object> args) {
        if (args.isEmpty() || !template.contains("${")) {
            return template;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String[] parts = placeholder.split("\\.");
            
            // Handle simple parameter references like ${email}
            if (parts.length == 1 && args.containsKey(parts[0])) {
                Object value = args.get(parts[0]);
                String replacement = maskIfSensitive(parts[0], value);
                // Escape special regex chars in the replacement
                replacement = Matcher.quoteReplacement(replacement);
                matcher.appendReplacement(result, replacement);
            } 
            // Handle nested properties like ${request.email}
            else if (parts.length > 1 && args.containsKey(parts[0])) {
                try {
                    Object value = getNestedProperty(args.get(parts[0]), parts, 1);
                    if (value != null) {
                        String replacement = maskIfSensitive(parts[parts.length-1], value);
                        // Escape special regex chars in the replacement
                        replacement = Matcher.quoteReplacement(replacement);
                        matcher.appendReplacement(result, replacement);
                        continue;
                    }
                } catch (Exception e) {
                    // If we can't resolve the property, leave placeholder as is
                }
            }
            
            // If we get here, keep the original placeholder but convert to a more readable form
            matcher.appendReplacement(result, Matcher.quoteReplacement("[" + placeholder + "]"));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Gets a nested property value using reflection
     */
    private Object getNestedProperty(Object obj, String[] parts, int index) {
        if (obj == null || index >= parts.length) {
            return obj;
        }
        
        try {
            String fieldName = parts[index];
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return getNestedProperty(field.get(obj), parts, index + 1);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Masks sensitive values
     */
    private String maskIfSensitive(String name, Object value) {
        String valueStr = String.valueOf(value);
        
        // Add any additional sensitive fields here
        if (name.toLowerCase().contains("password") || 
            name.toLowerCase().contains("token") ||
            name.toLowerCase().contains("secret") ||
            name.toLowerCase().contains("key")) {
            return "********";
        }
        
        return valueStr;
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