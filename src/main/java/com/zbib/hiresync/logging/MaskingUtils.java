package com.zbib.hiresync.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for masking sensitive data in logs
 */
@Component
@RequiredArgsConstructor
public class MaskingUtils {
    public static final String MASK = "********";
    
    // Patterns for sensitive data
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}[-]?\\d{2}[-]?\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password[=:])([^,}\\s\"]+)", Pattern.CASE_INSENSITIVE);
    
    // Default sensitive field names
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "passwd", "secret", "credential", "token", "auth", "key", 
            "apikey", "api_key", "ssn", "creditcard", "credit_card", "cc", "cvv", 
            "email", "phone", "address", "zipcode", "postalcode", "dob", "birthdate"
    ));
    
    private final ObjectMapper objectMapper;
    
    /**
     * Masks a string that may contain sensitive information
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // Mask credit card numbers
        value = replaceWithMask(value, CREDIT_CARD_PATTERN, "****-****-****-****");
        
        // Mask SSNs
        value = replaceWithMask(value, SSN_PATTERN, "***-**-****");
        
        // Mask emails
        value = replaceWithMask(value, EMAIL_PATTERN, "****@****.com");
        
        // Mask passwords
        value = replaceWithMask(value, PASSWORD_PATTERN, "$1" + MASK);
        
        return value;
    }

    /**
     * Masks an object by converting to JSON with sensitive data hidden
     */
    public String maskObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof String) {
            return mask((String) obj);
        }

        try {
            JsonNode jsonNode = objectMapper.valueToTree(obj);
            JsonNode maskedNode = maskJsonNode(jsonNode);
            return objectMapper.writeValueAsString(maskedNode);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
    
    /**
     * Recursively masks sensitive fields in a JSON node
     */
    private JsonNode maskJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = node.fieldNames();
            
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode childNode = node.get(fieldName);
                
                if (isSensitiveField(fieldName)) {
                    // This is a sensitive field, mask its value
                    objectNode.put(fieldName, MASK);
                } else if (childNode.isObject() || childNode.isArray()) {
                    // Recursively process objects and arrays
                    objectNode.set(fieldName, maskJsonNode(childNode));
                } else if (childNode.isTextual() && containsSensitiveData(childNode.asText())) {
                    // Check if text content is sensitive
                    objectNode.put(fieldName, MASK);
                }
            }
            return objectNode;
        } else if (node.isArray()) {
            // Process each array element
            for (int i = 0; i < node.size(); i++) {
                ((com.fasterxml.jackson.databind.node.ArrayNode) node).set(i, maskJsonNode(node.get(i)));
            }
        }
        return node;
    }
    
    /**
     * Determines if a field name indicates sensitive content
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        
        String normalizedName = fieldName.toLowerCase();
        
        for (String sensitive : SENSITIVE_FIELDS) {
            if (normalizedName.equals(sensitive) || normalizedName.contains(sensitive)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determines if a string value contains sensitive data patterns
     */
    private boolean containsSensitiveData(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        return CREDIT_CARD_PATTERN.matcher(value).find() ||
               SSN_PATTERN.matcher(value).find() ||
               EMAIL_PATTERN.matcher(value).find();
    }
    
    private String replaceWithMask(String input, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}