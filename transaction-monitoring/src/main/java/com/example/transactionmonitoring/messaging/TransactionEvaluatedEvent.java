package com.example.transactionmonitoring.messaging;

/**
 * Signals that the rule engine finished evaluating a transaction, carrying
 * the resulting alert count so subscribers can notify connected clients
 * once the evaluation has been committed to the database.
 */
public record TransactionEvaluatedEvent(Long transactionId, int alertCount) {
}
