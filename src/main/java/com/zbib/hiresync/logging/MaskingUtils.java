package com.zbib.hiresync.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for masking sensitive data in logs
 */
@Component
public class MaskingUtils {
    public static final String DEFAULT_MASK = "********";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([^@\\s]+)@([^@\\s]+)");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    
    private final Set<String> sensitiveFields;
    private final ObjectMapper objectMapper;

    public MaskingUtils(
            @Value("${hiresync.logging.sensitive-fields:password,token,secret,key,credential,ssn,creditcard,cardnumber,cvv,email,phone,address,zipcode,postalcode,dob,birthdate,socialsecurity}") String[] configSensitiveFields,
            ObjectMapper objectMapper) {
        this.sensitiveFields = new HashSet<>(Arrays.asList(configSensitiveFields));
        this.objectMapper = objectMapper;
    }

    /**
     * Mask a string that may contain sensitive information
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String masked = maskEmail(value);
        masked = maskCreditCard(masked);
        masked = maskPhoneNumber(masked);
        
        return masked;
    }

    /**
     * Mask an object by converting to string or JSON
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

    private String maskEmail(String input) {
        Matcher matcher = EMAIL_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String username = matcher.group(1);
            if (username.length() <= 2) {
                matcher.appendReplacement(sb, DEFAULT_MASK + "@" + matcher.group(2));
            } else {
                matcher.appendReplacement(sb, username.charAt(0) + DEFAULT_MASK + "@" + matcher.group(2));
            }
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskCreditCard(String input) {
        Matcher matcher = CREDIT_CARD_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String card = matcher.group().replaceAll("\\D", "");
            String masked = "****" + card.substring(Math.max(0, card.length() - 4));
            matcher.appendReplacement(sb, masked);
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskPhoneNumber(String input) {
        Matcher matcher = PHONE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String phone = matcher.group().replaceAll("\\D", "");
            String masked = "***-***-" + phone.substring(Math.max(0, phone.length() - 4));
            matcher.appendReplacement(sb, masked);
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Check if a field name should be considered sensitive
     */
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        
        String lowerField = fieldName.toLowerCase();
        for (String sensitiveField : sensitiveFields) {
            if (lowerField.contains(sensitiveField)) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isSensitive(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        return isSensitiveField(value);
    }

    private JsonNode maskJsonNode(JsonNode node) {
        if (node.isObject()) {
            return maskObjectNode((ObjectNode) node);
        } else if (node.isArray()) {
            return maskArrayNode((ArrayNode) node);
        }
        
        return node;
    }

    private ObjectNode maskObjectNode(ObjectNode objectNode) {
        ObjectNode result = objectNode.deepCopy();
        Iterator<String> fieldNames = result.fieldNames();
        
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = result.get(fieldName);
            
            if (isSensitiveField(fieldName)) {
                result.put(fieldName, DEFAULT_MASK);
            } else if (fieldValue.isObject() || fieldValue.isArray()) {
                result.set(fieldName, maskJsonNode(fieldValue));
            } else if (fieldValue.isTextual()) {
                result.put(fieldName, mask(fieldValue.asText()));
            }
        }
        
        return result;
    }

    private ArrayNode maskArrayNode(ArrayNode arrayNode) {
        ArrayNode result = arrayNode.deepCopy();
        
        for (int i = 0; i < result.size(); i++) {
            JsonNode element = result.get(i);
            
            if (element.isObject() || element.isArray()) {
                result.set(i, maskJsonNode(element));
            } else if (element.isTextual()) {
                result.set(i, objectMapper.valueToTree(mask(element.asText())));
            }
        }
        
        return result;
    }
}