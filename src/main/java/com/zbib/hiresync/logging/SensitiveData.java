package com.zbib.hiresync.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking fields that contain sensitive data that should be masked in logs.
 * Used with the MaskingUtils to automatically mask sensitive information.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {
    
    /**
     * Type of sensitive data, used to determine masking strategy.
     * Defaults to DEFAULT which masks the entire value.
     */
    SensitiveType type() default SensitiveType.DEFAULT;
    
    /**
     * Number of characters to show at the beginning of masked values.
     * Used for PARTIAL masking types. Default is 0 (show none).
     */
    int prefixLength() default 0;
    
    /**
     * Number of characters to show at the end of masked values.
     * Used for PARTIAL masking types. Default is 0 (show none).
     */
    int suffixLength() default 0;
    
    /**
     * Character to use for masking. Default is '*'.
     */
    char maskChar() default '*';
    
    /**
     * Custom regex pattern to identify sensitive parts of data.
     * Only used with PATTERN masking type.
     */
    String pattern() default "";
    
    /**
     * Flag to indicate if partial value should be shown in logs.
     * When true, some part of the sensitive data may be shown.
     */
    boolean showPartial() default false;
    
    /**
     * Types of sensitive data with different masking strategies.
     */
    enum SensitiveType {
        /** Completely mask the value */
        DEFAULT,
        
        /** Mask all except prefix/suffix */
        PARTIAL,
        
        /** Mask only portions matching a pattern */
        PATTERN,
        
        /** Special handling for emails (show domain) */
        EMAIL,
        
        /** Special handling for financial data */
        FINANCIAL,
        
        /** Special handling for personal identifying information */
        PII,
        
        /** Special handling for authentication credentials */
        CREDENTIALS
    }
} 