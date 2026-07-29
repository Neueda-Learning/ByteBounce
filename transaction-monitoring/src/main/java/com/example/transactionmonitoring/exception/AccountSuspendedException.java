package com.example.transactionmonitoring.exception;

/**
 * Raised when a transaction is attempted by an account suspended due to high risk.
 */
public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException(String message) {
        super(message);
    }
}
