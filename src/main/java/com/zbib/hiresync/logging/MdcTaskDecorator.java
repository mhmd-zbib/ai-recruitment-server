package com.zbib.hiresync.logging;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Decorator for async tasks to propagate MDC context
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {
    
    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture current context
        Map<String, String> contextMap = ThreadContext.getImmutableContext();
        
        return () -> {
            // Save previous context of the worker thread if any
            Map<String, String> previousContext = ThreadContext.getImmutableContext();
            
            try {
                // Apply the original context to this thread
                ThreadContext.clearAll();
                if (contextMap != null) {
                    contextMap.forEach(ThreadContext::put);
                }
                
                // Run the actual task
                runnable.run();
            } finally {
                // Restore the worker thread's previous context
                ThreadContext.clearAll();
                if (previousContext != null) {
                    previousContext.forEach(ThreadContext::put);
                }
            }
        };
    }
} 