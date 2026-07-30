package com.example.transactionmonitoring.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.transactionmonitoring.service.SseNotificationService;

import lombok.RequiredArgsConstructor;

/**
 * Pushes a real-time notification to connected clients once the rule-engine
 * evaluation for a transaction has been committed to the database, mirroring
 * the after-commit pattern used by {@link TransactionEventPublisher}.
 */
@Component
@RequiredArgsConstructor
public class TransactionEvaluatedEventListener {

    private final SseNotificationService sseNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionEvaluated(TransactionEvaluatedEvent event) {
        sseNotificationService.broadcastTransactionEvaluated(
                event.transactionId(),
                event.alertCount()
        );
    }
}
