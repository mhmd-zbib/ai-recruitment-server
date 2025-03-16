package com.zbib.hiresync.logging;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@Log4j2
public class LoggingAspect {

    private final RequestLogger requestLogger;
    private final ResponseFormatter responseFormatter;
    private final PerformanceMonitor performanceMonitor;
    
    public LoggingAspect(RequestLogger requestLogger, ResponseFormatter responseFormatter, 
                         PerformanceMonitor performanceMonitor) {
        this.requestLogger = requestLogger;
        this.responseFormatter = responseFormatter;
        this.performanceMonitor = performanceMonitor;
    }

    @Around("execution(* com.zbib.hiresync.controller.*.*(..))")
    public Object logControllerMethods(ProceedingJoinPoint pjp) throws Throwable {
        String correlationId = UUID.randomUUID().toString();
        String controllerName = pjp.getSignature().getDeclaringType().getSimpleName();
        String methodName = pjp.getSignature().getName();
        
        requestLogger.logRequest(correlationId, controllerName, methodName, pjp.getArgs());
        
        long startTime = performanceMonitor.startTimer();
        
        try {
            Object result = pjp.proceed();
            
            long executionTime = performanceMonitor.calculateExecutionTime(startTime);
            
            performanceMonitor.logPerformanceWarning(correlationId, executionTime, controllerName, methodName);
            
            String formattedResponse = responseFormatter.formatResponse(result);
            log.info("[{}] <- {}ms - {}", correlationId, executionTime, formattedResponse);
            
            return result;
        } catch (Exception e) {
            long executionTime = performanceMonitor.calculateExecutionTime(startTime);
            log.error("[{}] X {}ms: - {}", correlationId, executionTime, e.getMessage());
            throw e;
        }
    }


}