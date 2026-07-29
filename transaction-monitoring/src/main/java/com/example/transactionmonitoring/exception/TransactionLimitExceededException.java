package com.example.transactionmonitoring.exception;

/**
 * Raised when a medium-risk account attempts a transaction above its
 * restricted transaction limit.
 */
public class TransactionLimitExceededException extends RuntimeException {

    public TransactionLimitExceededException(String message) {
        super(message);
    }
}
