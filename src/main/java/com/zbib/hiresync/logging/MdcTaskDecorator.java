package com.zbib.hiresync.logging;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Decorator for asynchronous tasks that propagates MDC context across thread boundaries.
 * Ensures that logging context like correlation IDs, user IDs, and other diagnostic
 * information is properly maintained in asynchronous operations.
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {
    
    private final ThreadContextOperations threadContext;
    
    public MdcTaskDecorator(ThreadContextOperations threadContext) {
        this.threadContext = threadContext;
    }
    
    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture current thread's context
        final Map<String, String> contextMap = threadContext.getImmutableContext();
        
        // Return a wrapped Runnable that applies the context to the new thread
        return () -> {
            // Store the target thread's original context
            final Map<String, String> originalContext = threadContext.getImmutableContext();
            
            try {
                // Apply the source thread's context to this thread
                applyContext(contextMap);
                
                // Execute the actual task
                runnable.run();
            } finally {
                // Restore the thread's original context
                applyContext(originalContext);
            }
        };
    }
    
    /**
     * Applies the given context map to the current thread.
     */
    private void applyContext(Map<String, String> contextMap) {
        // Clear existing context first
        threadContext.clearAll();
        
        // Apply new context if not empty
        if (contextMap != null && !contextMap.isEmpty()) {
            contextMap.forEach(threadContext::put);
        }
    }
} 