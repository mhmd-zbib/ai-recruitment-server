package com.zbib.hiresync.logging;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(
            Arrays.asList("password", "secret", "token", "key", "credential", "pin", "ssn", "creditCard", "cardNumber",
                    "cvv", "securityCode"));

    public Object[] maskSensitiveData(Object[] args) {
        if (args == null) {
            return null;
        }

        Object[] maskedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            maskedArgs[i] = maskSensitiveData(args[i]);
        }
        return maskedArgs;
    }

    public Object maskSensitiveData(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof String || obj.getClass().isPrimitive() || isPrimitiveWrapper(obj.getClass())) {
            return obj;
        }

        try {
            String objString = obj.toString();

            for (String field : SENSITIVE_FIELDS) {
                objString = objString.replaceAll("(?i)" + field + "\\s*=\\s*[^,)\\s]+", field + "=******");
            }
            return objString;
        } catch (Exception e) {
            return obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode());
        }
    }

    private boolean isPrimitiveWrapper(Class<?> clazz) {
        return clazz == Boolean.class || clazz == Character.class || clazz == Byte.class || clazz == Short.class || 
               clazz == Integer.class || clazz == Long.class || clazz == Float.class || clazz == Double.class || 
               clazz == Void.class;
    }
}