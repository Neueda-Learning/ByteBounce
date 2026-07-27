package com.example.transactionmonitoring.entity;

/**
 * Monitoring rule types supported by the rule engine.
 */
public enum RuleType {
    AMOUNT_THRESHOLD,
    VELOCITY,
    NEW_PAYEE,
    DAILY_LIMIT
}
