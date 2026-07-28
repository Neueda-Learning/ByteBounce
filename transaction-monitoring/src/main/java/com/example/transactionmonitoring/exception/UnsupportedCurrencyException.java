package com.example.transactionmonitoring.exception;

/**
 * Raised when a transaction currency has no configured conversion rate.
 */
public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String message) {
        super(message);
    }
}