package com.zbib.hiresync.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LogManager.getLogger(LoggingAspect.class);
    private final ObjectMapper jacksonObjectMapper;

    public LoggingAspect(ObjectMapper jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }

    // Pointcut for methods annotated with @Loggable
    @Pointcut("@annotation(com.zbib.hiresync.logging.Loggable)")
    public void loggableMethods() {
    }

    // Before method execution logging
    @Before("loggableMethods()")
    public void logMethodEntry(JoinPoint joinPoint) throws JsonProcessingException {
        Object[] args = joinPoint.getArgs();
        String jsonArgs = jacksonObjectMapper.writeValueAsString(args[0]);

        ThreadContext.put("data", jsonArgs);
        String methodName = joinPoint.getSignature().getName();
        logger.info("Entering method: " + methodName);
    }

    // After method execution logging
    @AfterReturning(pointcut = "loggableMethods()", returning = "result")
    public void logMethodExit(JoinPoint joinPoint, Object result) throws JsonProcessingException {
        String jsonResult = jacksonObjectMapper.writeValueAsString(result);

        ThreadContext.put("data", jsonResult);
        String methodName = joinPoint.getSignature().getName();
        logger.info("Exiting method: " + methodName);
    }

    // After throwing exception logging
    @AfterThrowing(pointcut = "loggableMethods()", throwing = "exception")
    public void logMethodException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        logger.error("Exception in method: " + methodName, exception);
    }
}
