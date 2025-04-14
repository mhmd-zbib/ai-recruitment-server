package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for masking sensitive data in logs and JSON payloads.
 */
@Component
public class MaskingUtils {

    private static final Logger LOGGER = LogManager.getLogger(MaskingUtils.class);
    private static final String MASK = "********";
    
    private static final Set<String> SENSITIVE_TERMS = Set.of(
            "password", "token", "secret", "credential", "key", 
            "auth", "credit", "card", "cvv", "ssn", "social", 
            "passport", "license");
                     
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("^(?:\\d{4}[- ]?){3}\\d{4}$");
    
    private final Map<Class<?>, Map<String, SensitiveData>> annotationCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public MaskingUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Masks sensitive data in the given JSON string.
     */
    public String maskSensitiveData(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(json);
            maskSensitiveFields(rootNode);
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            LOGGER.debug("Failed to mask JSON data", e);
            return json;
        }
    }
    
    /**
     * Masks sensitive data in the given object using reflection and annotations.
     */
    public String maskSensitiveData(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(obj);
            Map<String, SensitiveData> sensitiveFields = getSensitiveFields(obj.getClass());
            
            if (sensitiveFields.isEmpty()) {
                return maskSensitiveData(json);
            }
            
            JsonNode rootNode = objectMapper.readTree(json);
            maskAnnotatedFields(rootNode, sensitiveFields);
            
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            LOGGER.debug("Failed to mask object data", e);
            try {
                return maskSensitiveData(objectMapper.writeValueAsString(obj));
            } catch (Exception ex) {
                return String.valueOf(obj);
            }
        }
    }

    /**
     * Checks if a field name contains a sensitive term.
     */
    public boolean containsSensitiveTerm(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        
        String normalizedName = fieldName.toLowerCase();
        return SENSITIVE_TERMS.stream().anyMatch(normalizedName::contains);
    }
    
    /**
     * Gets sensitive fields from cache or scans the class.
     */
    private Map<String, SensitiveData> getSensitiveFields(Class<?> clazz) {
        return annotationCache.computeIfAbsent(clazz, this::scanClassHierarchyForSensitiveData);
    }
    
    private Map<String, SensitiveData> scanClassHierarchyForSensitiveData(Class<?> clazz) {
        Map<String, SensitiveData> result = new ConcurrentHashMap<>();
        
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                SensitiveData annotation = field.getAnnotation(SensitiveData.class);
                if (annotation != null) {
                    result.put(field.getName(), annotation);
                }
            }
            clazz = clazz.getSuperclass();
        }
        
        return result;
    }
    
    private void maskAnnotatedFields(JsonNode node, Map<String, SensitiveData> sensitiveFields) {
        if (node == null) {
            return;
        }
        
        if (node.isObject()) {
            processObjectNodeWithAnnotations((ObjectNode)node, sensitiveFields);
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode)node;
            for (int i = 0; i < arrayNode.size(); i++) {
                maskAnnotatedFields(arrayNode.get(i), sensitiveFields);
            }
        }
    }
    
    private void processObjectNodeWithAnnotations(ObjectNode node, Map<String, SensitiveData> sensitiveFields) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            
            if (sensitiveFields.containsKey(fieldName) && fieldValue.isValueNode()) {
                node.put(fieldName, MASK);
            } else {
                if (fieldValue.isObject()) {
                    maskAnnotatedFields(fieldValue, sensitiveFields);
                } else if (fieldValue.isArray()) {
                    maskAnnotatedFields(fieldValue, sensitiveFields);
                }
            }
        }
    }
    
    private void maskSensitiveFields(JsonNode node) {
        if (node == null) {
            return;
        }
        
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = ((ObjectNode)node).fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey().toLowerCase();
                JsonNode childNode = entry.getValue();
                
                if (containsSensitiveTerm(fieldName) && childNode.isValueNode()) {
                    ((ObjectNode)node).put(entry.getKey(), MASK);
                } else {
                    maskSensitiveFields(childNode);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode)node;
            for (int i = 0; i < arrayNode.size(); i++) {
                maskSensitiveFields(arrayNode.get(i));
            }
        }
    }
    
    /**
     * Mask email addresses showing only the domain part.
     */
    public String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return MASK;
        }
        
        int atIndex = email.indexOf('@');
        return MASK + email.substring(atIndex);
    }
    
    /**
     * Mask credit card numbers showing only last 4 digits.
     */
    public String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return MASK;
        }
        
        String digitsOnly = cardNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 4) {
            return MASK;
        }
        
        return MASK + digitsOnly.substring(digitsOnly.length() - 4);
    }
    
    /**
     * Mask a string showing only prefix and suffix characters.
     */
    public String maskMiddle(String value, int prefixLen, int suffixLen) {
        if (value == null || value.isEmpty()) {
            return MASK;
        }
        
        if (prefixLen + suffixLen >= value.length()) {
            return MASK;
        }
        
        StringBuilder result = new StringBuilder();
        if (prefixLen > 0) {
            result.append(value.substring(0, prefixLen));
        }
        
        result.append(MASK);
        
        if (suffixLen > 0) {
            result.append(value.substring(value.length() - suffixLen));
        }
        
        return result.toString();
    }
}