package com.zbib.hiresync.logging;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PerformanceMonitor {

    public long startTimer() {
        return System.currentTimeMillis();
    }
    
    public long calculateExecutionTime(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
    
    public void logPerformanceWarning(String correlationId, long executionTime, String controllerName, String methodName) {
        if (executionTime > 1000) {
            log.warn("[{}] ! {}ms - {}.{}", correlationId, executionTime, controllerName, methodName);
        }
    }
}