package com.zbib.hiresync.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AOP-based logging aspect for methods annotated with @LoggableService
 * Follows the Chain of Responsibility pattern for log message construction
 * and the Strategy pattern for log level determination.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final String CORRELATION_ID = "correlationId";
    private final LogContextManager contextManager;
    private final LogMessageBuilder messageBuilder;
    private final LogLevelSelector logLevelSelector;
    private final LoggerAdapter loggerAdapter;
    private final ArgumentProcessor argumentProcessor;
    
    public LoggingAspect(MaskingUtils maskingUtils) {
        this.contextManager = new LogContextManager();
        this.messageBuilder = new LogMessageBuilder();
        this.logLevelSelector = new LogLevelSelector();
        this.loggerAdapter = new LoggerAdapter();
        this.argumentProcessor = new ArgumentProcessor(maskingUtils);
    }

    @Around("@annotation(com.zbib.hiresync.logging.LoggableService) || within(@com.zbib.hiresync.logging.LoggableService *)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        
        Logger logger = LogManager.getLogger(targetClass);
        
        LoggableService methodAnnotation = method.getAnnotation(LoggableService.class);
        LoggableService classAnnotation = targetClass.getAnnotation(LoggableService.class);
        LoggableService config = methodAnnotation != null ? methodAnnotation : classAnnotation;
        
        if (config == null) {
            return joinPoint.proceed();
        }
        
        String className = targetClass.getSimpleName();
        String methodName = method.getName();
        String customMessage = config.message();
        
        contextManager.setupContext(className, methodName);
        
        try {
            Map<String, String> argumentsMap = new HashMap<>();
            if (config.logArguments()) {
                argumentProcessor.processArguments(joinPoint, config, argumentsMap);
            }
            
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();
            
            long executionTime = stopWatch.getTotalTimeMillis();
            ThreadContext.put("executionTime", executionTime + "ms");
            
            String message = messageBuilder.buildSuccessMessage(methodName, customMessage, executionTime, argumentsMap);
            loggerAdapter.log(logger, config.level(), message);
            
            return result;
            
        } catch (Exception ex) {
            contextManager.registerException(ex);
            
            LogLevel logLevel = logLevelSelector.determineLogLevel(ex, config.level());
            String message = messageBuilder.buildErrorMessage(methodName, customMessage, ex);
            
            loggerAdapter.logException(logger, logLevel, message, ex);
            
            throw ex;
        } finally {
            contextManager.clearContext();
        }
    }
    
    /**
     * Strategy pattern implementation for selecting appropriate log level
     */
    static class LogLevelSelector {
        public LogLevel determineLogLevel(Exception ex, LogLevel defaultLevel) {
            if (isAuthenticationException(ex) || isValidationException(ex)) {
                return LogLevel.WARN;
            }
            return defaultLevel;
        }
        
        private boolean isAuthenticationException(Exception ex) {
            return ex instanceof BadCredentialsException || 
                   ex instanceof UsernameNotFoundException;
        }
        
        private boolean isValidationException(Exception ex) {
            if (ex == null || ex.getMessage() == null) return false;
            
            String message = ex.getMessage().toLowerCase();
            return message.contains("invalid email or password") ||
                   message.contains("validation") ||
                   message.contains("not found");
        }
        
        public boolean isExpectedException(Exception ex) {
            return isAuthenticationException(ex) || 
                   isValidationException(ex) ||
                   ex.getClass().getSimpleName().contains("NotFound");
        }
    }
    
    /**
     * Chain of Responsibility pattern for message building
     */
    static class LogMessageBuilder {
        private static final Pattern MESSAGE_TEMPLATE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
        
        public String buildSuccessMessage(String methodName, String customMessage, long executionTime, 
                                         Map<String, String> argumentsMap) {
            StringBuilder message = new StringBuilder();
            
            if (StringUtils.hasText(customMessage)) {
                String processedMessage = processMessageTemplate(customMessage, argumentsMap);
                message.append(processedMessage);
            } else {
                message.append(methodName);
                
                String args = ThreadContext.get("arguments");
                if (args != null && !args.isEmpty()) {
                    message.append("(").append(args).append(")");
                }
            }
            
            if (executionTime > 0) {
                message.append(" completed in ").append(executionTime).append("ms");
                
                if (executionTime > 800) {
                    message.append(" (SLOW)");
                }
            }
            
            return message.toString();
        }
        
        public String buildErrorMessage(String methodName, String customMessage, Exception ex) {
            StringBuilder message = new StringBuilder();
            
            if (StringUtils.hasText(customMessage)) {
                message.append(customMessage);
            } else {
                message.append(methodName);
                
                String args = ThreadContext.get("arguments");
                if (args != null && !args.isEmpty()) {
                    message.append("(").append(args).append(")");
                }
            }
            
            message.append(" failed: ").append(ex.getMessage());
            
            String errorCode = ThreadContext.get("errorCode");
            if (errorCode != null) {
                message.append(" [").append(errorCode).append("]");
            }
            
            return message.toString();
        }
        
        private String processMessageTemplate(String template, Map<String, String> args) {
            if (template == null || args == null || args.isEmpty()) {
                return template;
            }
            
            Matcher matcher = MESSAGE_TEMPLATE_PATTERN.matcher(template);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String key = matcher.group(1);
                String replacement = args.getOrDefault(key, matcher.group(0));
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            
            matcher.appendTail(result);
            return result.toString();
        }
    }
    
    /**
     * Adapter pattern for logging
     */
    static class LoggerAdapter {
        private final LogLevelSelector logLevelSelector = new LogLevelSelector();
        
        public void log(Logger logger, LogLevel logLevel, String message) {
            switch (logLevel) {
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
                case FATAL:
                    logger.fatal(message);
                    break;
                default:
                    logger.info(message);
            }
        }
        
        public void logException(Logger logger, LogLevel logLevel, String message, Exception ex) {
            // Avoid logging full stack trace for expected exceptions
            if (logLevelSelector.isExpectedException(ex)) {
                log(logger, logLevel, message);
            } else {
                if (logLevel == LogLevel.ERROR || logLevel == LogLevel.FATAL) {
                    logger.error(message, ex);
                } else if (logLevel == LogLevel.WARN) {
                    logger.warn(message);
                } else {
                    logger.info(message);
                }
            }
        }
    }
    
    /**
     * Context Manager for thread context
     */
    static class LogContextManager {
        public void setupContext(String className, String methodName) {
            ensureCorrelationId();
            ThreadContext.put("className", className);
            ThreadContext.put("methodName", methodName);
        }
        
        public void registerException(Exception ex) {
            String errorCode = "ERR-" + Math.abs(ex.getClass().getName().hashCode() % 10000);
            ThreadContext.put("exception", ex.getClass().getSimpleName());
            ThreadContext.put("errorCode", errorCode);
        }
        
        private void ensureCorrelationId() {
            String correlationId = ThreadContext.get(CORRELATION_ID);
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = "TX-" + UUID.randomUUID().toString().substring(0, 8);
                ThreadContext.put(CORRELATION_ID, correlationId);
            }
        }
        
        public void clearContext() {
            ThreadContext.remove("className");
            ThreadContext.remove("methodName");
            ThreadContext.remove("executionTime");
            ThreadContext.remove("arguments");
            ThreadContext.remove("exception");
            ThreadContext.remove("errorCode");
            // Don't clear correlationId as it might be needed across multiple method calls
        }
    }
    
    /**
     * Strategy pattern for argument processing
     */
    static class ArgumentProcessor {
        private final MaskingUtils maskingUtils;
        
        public ArgumentProcessor(MaskingUtils maskingUtils) {
            this.maskingUtils = maskingUtils;
        }
        
        public void processArguments(ProceedingJoinPoint joinPoint, LoggableService config, Map<String, String> argumentsMap) {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) return;
            
            String[] paramNames = ((MethodSignature)joinPoint.getSignature()).getParameterNames();
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                
                if (args[i] == null) {
                    sb.append("null");
                    if (paramNames != null && i < paramNames.length) {
                        argumentsMap.put(paramNames[i], "null");
                    }
                    continue;
                }
                
                String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
                
                if (isSensitiveParameter(paramName, config)) {
                    sb.append("******");
                    argumentsMap.put(paramName, "******");
                } else if (args[i] instanceof String || args[i] instanceof Number || args[i] instanceof Boolean) {
                    sb.append(args[i]);
                    argumentsMap.put(paramName, args[i].toString());
                } else {
                    // For complex objects, use MaskingUtils to safely convert to string with masked sensitive data
                    String maskedValue = maskingUtils.maskSensitiveData(args[i]);
                    // Just use parameter name in the log text
                    sb.append(paramName);
                    // But store the masked value in the arguments map for template substitution
                    argumentsMap.put(paramName, maskedValue != null ? maskedValue : paramName);
                }
            }
            
            if (sb.length() > 0) {
                ThreadContext.put("arguments", sb.toString());
            }
        }
        
        private boolean isSensitiveParameter(String paramName, LoggableService config) {
            if (paramName == null) return false;
            
            String name = paramName.toLowerCase();
            
            return Arrays.stream(config.sensitiveFields())
                    .anyMatch(field -> name.contains(field.toLowerCase()));
        }
    }
}