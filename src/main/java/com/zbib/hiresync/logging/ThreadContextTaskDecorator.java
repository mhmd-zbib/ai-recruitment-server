package com.zbib.hiresync.logging;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Decorator for asynchronous tasks to propagate ThreadContext values.
 * <p>
 * This ensures consistent logging context across thread boundaries, allowing
 * correlationIds and other context to follow requests into async tasks.
 */
@Component
public class ThreadContextTaskDecorator implements TaskDecorator {
    
    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture the current context before the task executes
        Map<String, String> contextMap = ThreadContext.getContext();
        
        return () -> {
            // Restore context in the worker thread
            Map<String, String> previousContext = ThreadContext.getContext();
            
            try {
                // Clear any existing context in the new thread
                ThreadContext.clearAll();
                
                // Copy context from original thread
                if (contextMap != null) {
                    contextMap.forEach(ThreadContext::put);
                }
                
                // Add thread-specific context
                ThreadContext.put("asyncThread", Thread.currentThread().getName());
                
                // Execute the task
                runnable.run();
            } finally {
                // Clean up
                ThreadContext.clearAll();
                
                // Restore any previous context of the worker thread
                if (previousContext != null) {
                    previousContext.forEach(ThreadContext::put);
                }
            }
        };
    }
} 