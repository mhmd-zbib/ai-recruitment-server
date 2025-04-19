package com.zbib.hiresync.logging;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for working with MDC (Mapped Diagnostic Context) for structured logging.
 * Provides a clean API for managing thread-local context values across the application.
 */
public final class MdcContext {

    private static final ThreadContextOperations threadContext = new Log4jThreadContextAdapter();
    
    private MdcContext() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Adds a key-value pair to the MDC context.
     */
    public static void put(String key, String value) {
        threadContext.put(key, value);
    }
    
    /**
     * Retrieves a value from the MDC context.
     */
    public static String get(String key) {
        return threadContext.get(key);
    }
    
    /**
     * Removes a key from the MDC context.
     */
    public static void remove(String key) {
        threadContext.remove(key);
    }
    
    /**
     * Clears all MDC context data.
     */
    public static void clear() {
        threadContext.clearAll();
    }
    
    /**
     * Executes code with a temporary MDC context value, restoring the previous value afterward.
     * 
     * @param key The context key
     * @param value The context value
     * @param supplier The code to execute with the temporary context
     * @return The result of the supplier execution
     */
    public static <T> T with(String key, String value, Supplier<T> supplier) {
        String oldValue = threadContext.get(key);
        try {
            threadContext.put(key, value);
            return supplier.get();
        } finally {
            if (oldValue != null) {
                threadContext.put(key, oldValue);
            } else {
                threadContext.remove(key);
            }
        }
    }
    
    /**
     * Executes code with a completely replaced MDC context, restoring the previous context afterward.
     * 
     * @param context Map of context key-values to use
     * @param supplier The code to execute with the temporary context
     * @return The result of the supplier execution
     */
    public static <T> T withContext(Map<String, String> context, Supplier<T> supplier) {
        Map<String, String> oldContext = threadContext.getImmutableContext();
        try {
            threadContext.clearAll();
            if (context != null && !context.isEmpty()) {
                context.forEach(threadContext::put);
            }
            return supplier.get();
        } finally {
            threadContext.clearAll();
            if (oldContext != null && !oldContext.isEmpty()) {
                oldContext.forEach(threadContext::put);
            }
        }
    }
    
    /**
     * Adds business metadata to the MDC context with proper prefix.
     */
    public static void addMetadata(String key, String value) {
        put(ContextKeys.META_PREFIX + key, value);
    }
} 