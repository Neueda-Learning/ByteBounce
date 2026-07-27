package com.example.transactionmonitoring.exception;

/**
 * Raised when an alert lifecycle transition is not permitted.
 */
public class InvalidAlertStatusTransitionException extends RuntimeException {

    public InvalidAlertStatusTransitionException(String message) {
        super(message);
    }
}
