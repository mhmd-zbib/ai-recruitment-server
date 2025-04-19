package com.zbib.hiresync.logging;

import java.util.Map;

/**
 * Interface defining operations for thread-local context management.
 * <p>
 * Abstracts MDC operations to enable substitution of different implementation
 * strategies, facilitate testing, and follow dependency inversion.
 * <p>
 * This is a core component of the logging infrastructure that enables
 * contextual logging across threads and execution boundaries.
 */
public interface ThreadContextOperations {
    
    /**
     * Adds a value to the current thread's context.
     *
     * @param key The context key
     * @param value The value to store
     */
    void put(String key, String value);
    
    /**
     * Retrieves a value from the current thread's context.
     *
     * @param key The context key
     * @return The value or null if not found
     */
    String get(String key);
    
    /**
     * Removes a key from the current thread's context.
     *
     * @param key The context key to remove
     */
    void remove(String key);
    
    /**
     * Clears all data from the current thread's context.
     */
    void clearAll();
    
    /**
     * Returns an immutable snapshot of the current thread's context.
     *
     * @return Map containing the current context (never null)
     */
    Map<String, String> getImmutableContext();
} 