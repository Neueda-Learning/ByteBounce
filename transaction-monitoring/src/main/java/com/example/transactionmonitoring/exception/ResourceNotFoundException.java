package com.example.transactionmonitoring.exception;

/**
 * Raised when a requested application resource does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
