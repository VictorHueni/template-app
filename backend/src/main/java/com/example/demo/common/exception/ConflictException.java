package com.example.demo.common.exception;

/**
 * Exception thrown when a request conflicts with the current state of the resource.
 * Results in HTTP 409 Conflict with RFC 7807 Problem Details.
 *
 * Examples:
 * - Attempting to create a resource that already exists
 * - Concurrent modification conflicts
 * - Business rule conflicts (e.g., deleting a resource that has dependencies)
 */
public class ConflictException extends RuntimeException {

    private final String conflictReason;

    public ConflictException(String message) {
        super(message);
        this.conflictReason = null;
    }

    public ConflictException(String message, String conflictReason) {
        super(message);
        this.conflictReason = conflictReason;
    }

    public String getConflictReason() {
        return conflictReason;
    }
}

