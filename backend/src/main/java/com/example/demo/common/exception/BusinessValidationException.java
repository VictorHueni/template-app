package com.example.demo.common.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when business validation rules are violated.
 * Results in HTTP 400 Bad Request with RFC 7807 Problem Details.
 */
public class BusinessValidationException extends RuntimeException {

    private final Map<String, String> validationErrors = new HashMap<>();

    public BusinessValidationException(String message) {
        super(message);
    }

    public BusinessValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        if (validationErrors != null) {
            this.validationErrors.putAll(validationErrors);
        }
    }

    public BusinessValidationException(String field, String error) {
        super("Validation failed");
        this.validationErrors.put(field, error);
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}

