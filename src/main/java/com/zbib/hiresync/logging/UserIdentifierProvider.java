package com.zbib.hiresync.logging;

/**
 * Interface for providing user identifiers for logging purposes.
 * Implementations can extract user IDs from various sources such as 
 * security contexts, tokens, custom authentication mechanisms, etc.
 */
public interface UserIdentifierProvider {
    
    String getCurrentUserId();

    default String getUserContext() {
        return null;
    }
} 