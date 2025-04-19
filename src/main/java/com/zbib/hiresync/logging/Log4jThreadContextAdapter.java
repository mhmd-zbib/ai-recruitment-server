package com.zbib.hiresync.logging;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of ThreadContextOperations that adapts Log4j2's ThreadContext.
 * <p>
 * This adapter serves as a bridge between our application's thread context
 * abstraction and Log4j2's specific implementation, allowing us to swap
 * logging frameworks without changing application code.
 */
@Component
public class Log4jThreadContextAdapter implements ThreadContextOperations {
    
    @Override
    public void put(String key, String value) {
        if (key != null && value != null) {
            ThreadContext.put(key, value);
        }
    }
    
    @Override
    public String get(String key) {
        return key != null ? ThreadContext.get(key) : null;
    }
    
    @Override
    public void remove(String key) {
        if (key != null) {
            ThreadContext.remove(key);
        }
    }
    
    @Override
    public void clearAll() {
        ThreadContext.clearAll();
    }
    
    @Override
    public Map<String, String> getImmutableContext() {
        Map<String, String> context = ThreadContext.getContext();
        return context != null && !context.isEmpty() 
                ? Collections.unmodifiableMap(context) 
                : Collections.emptyMap();
    }
} 