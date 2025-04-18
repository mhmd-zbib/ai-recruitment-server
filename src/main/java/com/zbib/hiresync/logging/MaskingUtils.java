package com.zbib.hiresync.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise-grade implementation for masking sensitive data in logs with high performance
 * and resilience.
 */
@Log4j2
@Component
public class MaskingUtils {
    private static final String DEFAULT_MASK = "********";
    
    // Common patterns to mask
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9_.-]+)@([a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+\\d{1,2}\\s?)?(?:\\(\\d{3}\\)|\\d{3})[-\\s]?\\d{3}[-\\s]?\\d{4}\\b");
    
    private final Set<String> sensitiveFields;
    private final ObjectMapper objectMapper;
    
    public MaskingUtils(
            ObjectMapper objectMapper,
            @Value("${hiresync.logging.sensitive-fields:password,passwd,secret,credential,token,apikey,api_key,key,auth,jwt}") 
            String sensitiveFieldsList) {
        this.objectMapper = objectMapper;
        this.sensitiveFields = new HashSet<>(Arrays.asList(sensitiveFieldsList.split(",")));
    }
    
    /**
     * Mask sensitive data in a string
     */
    public String mask(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        try {
            // Try to mask as JSON if it looks like JSON
            if (isJson(data)) {
                return maskJson(data);
            }
            
            // Otherwise apply pattern masking
            return maskPatterns(data);
        } catch (Exception e) {
            log.debug("Error masking data: {}", e.getMessage());
            return data; // Return original data if masking fails
        }
    }
    
    /**
     * Mask an object by converting to string or JSON
     */
    public String maskObject(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        try {
            // Handle strings directly
            if (obj instanceof String) {
                return mask((String) obj);
            }
            
            // Convert to JSON and mask
            String json = objectMapper.writeValueAsString(obj);
            return maskJson(json);
        } catch (Exception e) {
            log.debug("Error masking object: {}", e.getMessage());
            return obj.toString(); // Return string representation if masking fails
        }
    }
    
    /**
     * Check if a field name should be masked
     */
    public boolean isSensitive(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        
        String lowerName = fieldName.toLowerCase();
        return sensitiveFields.stream().anyMatch(lowerName::contains);
    }
    
    /**
     * Mask sensitive data patterns in a string
     */
    private String maskPatterns(String data) {
        String result = data;
        
        // Mask email addresses - show domain only
        result = EMAIL_PATTERN.matcher(result).replaceAll("****@$2");
        
        // Mask credit card numbers
        result = CARD_PATTERN.matcher(result).replaceAll(match -> {
            String digitsOnly = match.group().replaceAll("[^0-9]", "");
            if (digitsOnly.length() < 13) return DEFAULT_MASK;
            
            return digitsOnly.substring(0, 6) + 
                   DEFAULT_MASK + 
                   digitsOnly.substring(Math.max(digitsOnly.length() - 4, 6));
        });
        
        // Mask phone numbers - show last 4 digits only
        result = PHONE_PATTERN.matcher(result).replaceAll(match -> {
            String digitsOnly = match.group().replaceAll("[^0-9]", "");
            return "****" + digitsOnly.substring(Math.max(0, digitsOnly.length() - 4));
        });
        
        return result;
    }
    
    /**
     * Mask sensitive fields in JSON
     */
    private String maskJson(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            maskJsonNode(rootNode);
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            // If JSON parsing fails, apply regular masking
            return maskPatterns(json);
        }
    }
    
    /**
     * Recursively mask sensitive fields in a JSON node
     */
    private void maskJsonNode(JsonNode node) {
        if (node == null) return;
        
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode valueNode = entry.getValue();
                
                if (isSensitive(fieldName) && valueNode.isValueNode()) {
                    ((ObjectNode) node).put(fieldName, DEFAULT_MASK);
                } else if (valueNode.isContainerNode()) {
                    maskJsonNode(valueNode);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                maskJsonNode(item);
            }
        }
    }
    
    private boolean isJson(String data) {
        if (data == null) return false;
        
        String trimmed = data.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}