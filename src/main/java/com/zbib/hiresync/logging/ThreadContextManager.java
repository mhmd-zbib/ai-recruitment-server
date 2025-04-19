package com.zbib.hiresync.logging;

import org.apache.logging.log4j.ThreadContext;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility for managing request context data in structured logging
 * using Log4j2 ThreadContext directly
 */
public final class ThreadContextManager {
    
    private ThreadContextManager() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Adds a key-value pair to the context
     */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            ThreadContext.put(key, value);
        }
    }
    
    /**
     * Retrieves a value from the context
     */
    public static String get(String key) {
        return key != null ? ThreadContext.get(key) : null;
    }
    
    /**
     * Removes a key from the context
     */
    public static void remove(String key) {
        if (key != null) {
            ThreadContext.remove(key);
        }
    }
    
    /**
     * Clears all context data
     */
    public static void clear() {
        ThreadContext.clearAll();
    }
    
    /**
     * Executes code with a temporary context value, restoring the previous value afterward
     */
    public static <T> T with(String key, String value, Supplier<T> supplier) {
        String oldValue = ThreadContext.get(key);
        try {
            ThreadContext.put(key, value);
            return supplier.get();
        } finally {
            if (oldValue != null) {
                ThreadContext.put(key, oldValue);
            } else {
                ThreadContext.remove(key);
            }
        }
    }
    
    /**
     * Executes code with a completely replaced context, restoring the previous context afterward
     */
    public static <T> T withContext(Map<String, String> context, Supplier<T> supplier) {
        Map<String, String> oldContext = ThreadContext.getContext();
        try {
            ThreadContext.clearAll();
            if (context != null && !context.isEmpty()) {
                context.forEach(ThreadContext::put);
            }
            return supplier.get();
        } finally {
            ThreadContext.clearAll();
            if (oldContext != null && !oldContext.isEmpty()) {
                oldContext.forEach(ThreadContext::put);
            }
        }
    }
    
    /**
     * Add structured context data with namespacing
     */
    public static void putStructuredData(String id, Map<String, String> data) {
        if (id != null && data != null && !data.isEmpty()) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    ThreadContext.put(id + "." + entry.getKey(), entry.getValue());
                }
            }
        }
    }
} 