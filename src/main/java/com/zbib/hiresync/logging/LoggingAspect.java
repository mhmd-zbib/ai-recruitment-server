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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LogManager.getLogger(LoggingAspect.class);
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";

    @Pointcut("@annotation(com.zbib.hiresync.logging.LoggableService)")
    public void loggableMethod() {
    }

    @Around("loggableMethod()")
    public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        setupLoggingContext();
        
        // Get method information
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = signature.getName();
        String className = signature.getDeclaringType().getSimpleName();
        
        // Get annotation information
        LoggableService loggableService = method.getAnnotation(LoggableService.class);
        String messageTemplate = loggableService.message().isEmpty() ? 
                "Executing method" : loggableService.message();
        
        // Process dynamic placeholders in the message
        String message = processDynamicMessage(messageTemplate, method, joinPoint.getArgs());
        
        // Get user and correlation IDs
        String userId = ThreadContext.get(USER_ID);
        String correlationId = ThreadContext.get(CORRELATION_ID);
        
        // Start timing
        long startTime = System.currentTimeMillis();
        
        try {
            // Execute the method
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log only with user ID and correlation ID
            log.info("{} - {}.{}() - completed in {}ms - userId: {} - correlationId: {}",
                    message, className, methodName, executionTime, userId, correlationId);
                    
            return result;
        } catch (Exception e) {
            // Log error
            log.error("{} - {}.{}() - FAILED - error: {} - userId: {} - correlationId: {}",
                    message, className, methodName, e.getMessage(), userId, correlationId, e);
            throw e;
        } finally {
            clearLoggingContext();
        }
    }
    
    private void setupLoggingContext() {
        // Only set up context if not already done
        if (ThreadContext.get(CORRELATION_ID) == null) {
            HttpServletRequest request = getRequest();
            String correlationId = null;
            
            // Check if correlation ID is in the request header
            if (request != null) {
                correlationId = request.getHeader("X-Correlation-ID");
            }
            
            // If not found in header, generate a new one
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            ThreadContext.put(CORRELATION_ID, correlationId);
        }
        
        // Set user ID from security context if authenticated
        if (ThreadContext.get(USER_ID) == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                    !"anonymousUser".equals(authentication.getPrincipal().toString())) {
                ThreadContext.put(USER_ID, authentication.getName());
            } else {
                ThreadContext.put(USER_ID, "anonymous");
            }
        }
    }
    
    private void clearLoggingContext() {
        ThreadContext.remove(CORRELATION_ID);
        ThreadContext.remove(USER_ID);
    }
    
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("Could not get HttpServletRequest: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Process dynamic placeholders in the message template.
     * Supports placeholders in the format ${paramName} where paramName is the name of a method parameter.
     *
     * @param messageTemplate the message template with placeholders
     * @param method the method being logged
     * @param args the method arguments
     * @return the processed message with placeholders replaced by actual values
     */
    private String processDynamicMessage(String messageTemplate, Method method, Object[] args) {
        if (!messageTemplate.contains("${")){
            return messageTemplate;
        }
        
        String result = messageTemplate;
        Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter param = parameters[i];
            Object arg = args[i];
            String paramName = param.getName();
            String placeholder = "${" + paramName + "}";
            
            if (result.contains(placeholder)) {
                String value = arg != null ? arg.toString() : "null";
                
                // For security, mask sensitive data like passwords
                if (paramName.toLowerCase().contains("password") || 
                    paramName.toLowerCase().contains("secret") || 
                    paramName.toLowerCase().contains("token")) {
                    value = "*****";
                }
                
                result = result.replace(placeholder, value);
            }
            
            // Handle nested properties for objects (e.g., ${request.email})
            if (arg != null) {
                result = processNestedProperties(result, paramName, arg);
            }
        }
        
        return result;
    }
    
    /**
     * Process nested properties in objects for dynamic placeholders.
     * Supports placeholders like ${request.email} for request objects.
     *
     * @param messageTemplate the message template with placeholders
     * @param paramName the parameter name
     * @param arg the parameter value
     * @return the processed message with nested placeholders replaced
     */
    private String processNestedProperties(String messageTemplate, String paramName, Object arg) {
        String result = messageTemplate;
        
        // Look for patterns like ${paramName.property}
        int startIdx = 0;
        String searchPattern = "${" + paramName + ".";
        while ((startIdx = result.indexOf(searchPattern, startIdx)) != -1) {
            int endIdx = result.indexOf("}", startIdx);
            if (endIdx == -1) break;
            
            String placeholder = result.substring(startIdx, endIdx + 1);
            String propertyPath = placeholder.substring(searchPattern.length(), placeholder.length() - 1);
            
            // Get the property value using reflection
            try {
                String value = getPropertyValue(arg, propertyPath);
                result = result.replace(placeholder, value);
            } catch (Exception e) {
                // If property access fails, leave the placeholder unchanged
                log.debug("Failed to access property: {} on object: {}", propertyPath, arg.getClass().getSimpleName());
            }
            
            startIdx = endIdx + 1;
        }
        
        return result;
    }
    
    /**
     * Get a property value from an object using reflection.
     *
     * @param obj the object to get the property from
     * @param propertyPath the path to the property (can be nested, e.g., "user.address.city")
     * @return the property value as a string
     */
    private String getPropertyValue(Object obj, String propertyPath) {
        try {
            String[] properties = propertyPath.split("\\.", -1);
            Object current = obj;
            
            for (String property : properties) {
                if (current == null) return "null";
                
                // Handle special case for common request types
                if (current instanceof HttpServletRequest && property.equals("email")) {
                    current = ((HttpServletRequest) current).getParameter("email");
                    continue;
                }
                
                // Try to find a getter method first
                String getterName = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
                try {
                    Method getter = current.getClass().getMethod(getterName);
                    current = getter.invoke(current);
                } catch (NoSuchMethodException e) {
                    // If no getter, try direct field access
                    try {
                        Field field = current.getClass().getDeclaredField(property);
                        field.setAccessible(true);
                        current = field.get(current);
                    } catch (NoSuchFieldException ex) {
                        // If field doesn't exist, check if it's a map
                        if (current instanceof Map) {
                            current = ((Map<?, ?>) current).get(property);
                        } else {
                            return "[unknown property: " + property + "]";
                        }
                    }
                }
            }
            
            // Mask sensitive data
            if (propertyPath.toLowerCase().contains("password") || 
                propertyPath.toLowerCase().contains("secret") || 
                propertyPath.toLowerCase().contains("token")) {
                return "*****";
            }
            
            return current != null ? current.toString() : "null";
        } catch (Exception e) {
            log.debug("Error accessing property: {}", propertyPath, e);
            return "[error]";
        }
    }
}
