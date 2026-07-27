package com.example.transactionmonitoring.exception;

/**
 * Raised when required parameters for a rule type are missing or invalid.
 */
public class InvalidRuleConfigurationException extends RuntimeException {

    public InvalidRuleConfigurationException(String message) {
        super(message);
    }
}
