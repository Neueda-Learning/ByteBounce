package com.example.transactionmonitoring.messaging;

/**
 * Signals that a new transaction has been persisted and is ready for
 * (asynchronous) rule evaluation. Used both as a local Spring application
 * event (published only after the enclosing DB transaction commits) and as
 * the JSON payload sent over RabbitMQ.
 */
public record TransactionCreatedEvent(Long transactionId) {
}
