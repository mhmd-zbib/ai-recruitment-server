package com.zbib.hiresync.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides method-level logging via AOP
 */
@Aspect
@Component
public class LoggingAspect {
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    
    private final MaskingUtils maskingUtils;
    private final UserIdentifierProvider userIdentifierProvider;
    
    @Value("${hiresync.logging.execution-time-threshold:500}")
    private long executionTimeThreshold;

    public LoggingAspect(MaskingUtils maskingUtils, UserIdentifierProvider userIdentifierProvider) {
        this.maskingUtils = maskingUtils;
        this.userIdentifierProvider = userIdentifierProvider;
    }

    @Around("@annotation(com.zbib.hiresync.logging.LoggableService) || within(@com.zbib.hiresync.logging.LoggableService *)")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        
        // Get logging config
        LoggableService config = getLoggingConfig(method, targetClass);
        if (config == null) {
            return joinPoint.proceed();
        }
        
        // Get logger
        Logger logger = LogManager.getLogger(targetClass);
        
        // Setup context
        Map<String, String> contextMap = new HashMap<>();
        
        // Add correlation ID
        String correlationId = ThreadContext.get(CORRELATION_ID);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }
        contextMap.put(CORRELATION_ID, correlationId);
        
        // Add class and method info
        contextMap.put("class", targetClass.getSimpleName());
        contextMap.put("method", method.getName());
        
        // Add user ID
        String userId = userIdentifierProvider.getCurrentUserId();
        if (userId != null) {
            contextMap.put(USER_ID, userId);
        }
        
        // Store parameter values for message interpolation
        Map<String, Object> paramValues = new HashMap<>();
        
        try {
            // Log method arguments
            if (config.logArguments()) {
                paramValues = logArguments(joinPoint, config.sensitiveFields(), contextMap);
            }
            
            // Execute method with timing
            StopWatch timer = new StopWatch();
            timer.start();
            Object result = joinPoint.proceed();
            timer.stop();
            
            // Record execution time
            long executionTime = timer.getTotalTimeMillis();
            contextMap.put("executionTime", String.valueOf(executionTime));
            
            // Apply MDC context and log
            applyContext(contextMap);
            logMethodSuccess(logger, config, method.getName(), executionTime, paramValues);
            
            return result;
        } catch (Exception ex) {
            // Log exception
            contextMap.put("exception", ex.getClass().getSimpleName());
            contextMap.put("errorMessage", ex.getMessage());
            
            applyContext(contextMap);
            logMethodFailure(logger, config, method.getName(), ex, paramValues);
            
            throw ex;
        } finally {
            ThreadContext.clearAll();
        }
    }
    
    private LoggableService getLoggingConfig(Method method, Class<?> targetClass) {
        LoggableService methodAnnotation = method.getAnnotation(LoggableService.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return targetClass.getAnnotation(LoggableService.class);
    }
    
    private void applyContext(Map<String, String> contextMap) {
        if (contextMap != null) {
            contextMap.forEach(ThreadContext::put);
        }
    }
    
    private Map<String, Object> logArguments(ProceedingJoinPoint joinPoint, String[] sensitiveFields, 
            Map<String, String> contextMap) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        if (paramNames == null || args == null || paramNames.length == 0) {
            return new HashMap<>();
        }
        
        Map<String, Object> paramValues = new HashMap<>();
        StringBuilder argsLog = new StringBuilder();
        
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            if (i > 0) {
                argsLog.append(", ");
            }
            
            String paramName = paramNames[i];
            Object value = args[i];
            
            // Store original value for message interpolation
            paramValues.put(paramName, value);
            
            // Check if sensitive
            boolean isSensitive = isSensitiveParameter(paramName, sensitiveFields);
            
            String strValue = isSensitive 
                ? maskingUtils.mask(value != null ? value.toString() : "null")
                : maskingUtils.maskObject(value);
            
            argsLog.append(paramName).append("=").append(strValue);
        }
        
        contextMap.put("arguments", argsLog.toString());
        return paramValues;
    }
    
    private boolean isSensitiveParameter(String paramName, String[] sensitiveFields) {
        if (paramName == null || paramName.isEmpty() || sensitiveFields == null || sensitiveFields.length == 0) {
            return false;
        }
        
        String lowerName = paramName.toLowerCase();
        for (String field : sensitiveFields) {
            if (lowerName.contains(field.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private void logMethodSuccess(Logger logger, LoggableService config, String methodName, 
            long executionTime, Map<String, Object> paramValues) {
        
        String message;
        if (config.message() != null && !config.message().isEmpty()) {
            message = replacePlaceholders(config.message(), paramValues);
        } else {
            StringBuilder msgBuilder = new StringBuilder(methodName);
            
            String args = ThreadContext.get("arguments");
            if (args != null && !args.isEmpty()) {
                msgBuilder.append("(").append(args).append(")");
            }
            
            msgBuilder.append(" completed in ").append(executionTime).append("ms");
            if (executionTime > executionTimeThreshold) {
                msgBuilder.append(" (SLOW)");
            }
            
            message = msgBuilder.toString();
        }
        
        logByLevel(logger, config.level(), message);
    }
    
    private void logMethodFailure(Logger logger, LoggableService config, String methodName, 
            Exception ex, Map<String, Object> paramValues) {
        
        String message;
        if (config.message() != null && !config.message().isEmpty()) {
            message = replacePlaceholders(config.message(), paramValues) + " failed: " + ex.getMessage();
        } else {
            message = methodName + " failed: " + ex.getMessage();
        }
        
        if (isBusinessException(ex)) {
            logger.warn(message);
        } else {
            logger.error(message, ex);
        }
    }
    
    private void logByLevel(Logger logger, LogLevel level, String message) {
        switch (level) {
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
            default:
                logger.info(message);
        }
    }
    
    private String replacePlaceholders(String template, Map<String, Object> paramValues) {
        if (template == null || template.isEmpty() || paramValues == null || paramValues.isEmpty()) {
            return template;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = paramValues.get(placeholder);
            
            String replacement = value != null ? maskingUtils.maskObject(value) : "null";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    private boolean isBusinessException(Exception ex) {
        if (ex == null) {
            return false;
        }
        
        String className = ex.getClass().getName().toLowerCase();
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        
        return className.contains("notfound") || 
               className.contains("validation") ||
               className.contains("security") || 
               message.contains("not found") ||
               message.contains("invalid");
    }
}