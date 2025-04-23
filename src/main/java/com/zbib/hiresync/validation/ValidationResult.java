package com.zbib.hiresync.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ValidationResult {
    
    private final List<String> errors;
    
    private ValidationResult(List<String> errors) {
        this.errors = errors;
    }
    
    public static ValidationResult valid() {
        return new ValidationResult(Collections.emptyList());
    }
    
    public static ValidationResult invalid(String error) {
        return new ValidationResult(Collections.singletonList(error));
    }
    
    public static ValidationBuilder startValidation() {
        return new ValidationBuilder();
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    public ValidationResult combine(ValidationResult other) {
        if (this.isValid() && other.isValid()) {
            return ValidationResult.valid();
        }
        
        List<String> combinedErrors = new ArrayList<>(this.errors);
        combinedErrors.addAll(other.errors);
        return new ValidationResult(combinedErrors);
    }
    
    public void throwIfInvalid(Supplier<RuntimeException> exceptionSupplier) {
        if (!isValid()) {
            throw exceptionSupplier.get();
        }
    }
    
    public static class ValidationBuilder {
        private final List<String> errors = new ArrayList<>();
        
        public ValidationBuilder validateThat(boolean condition, String errorMessage) {
            if (!condition) {
                errors.add(errorMessage);
            }
            return this;
        }
        
        public <T> ValidationBuilder validateThat(T value, Predicate<T> condition, String errorMessage) {
            if (!condition.test(value)) {
                errors.add(errorMessage);
            }
            return this;
        }
        
        public ValidationBuilder validateNotNull(Object value, String fieldName) {
            if (value == null) {
                errors.add(fieldName + " cannot be null");
            }
            return this;
        }
        
        public ValidationBuilder validateNotEmpty(String value, String fieldName) {
            if (value == null || value.trim().isEmpty()) {
                errors.add(fieldName + " cannot be empty");
            }
            return this;
        }
        
        public ValidationResult build() {
            return errors.isEmpty() ? ValidationResult.valid() : new ValidationResult(errors);
        }
    }
} 