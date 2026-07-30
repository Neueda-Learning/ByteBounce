package com.example.transactionmonitoring.service;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * Keeps track of connected Server-Sent-Events clients and pushes real-time
 * notifications to them so the frontend can refresh its data without
 * requiring a manual reload.
 */
@Slf4j
@Service
public class SseNotificationService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(throwable -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * Notifies every connected client that the rule engine finished
     * evaluating a transaction, so views showing transactions or alerts can
     * silently refresh themselves.
     */
    public void broadcastTransactionEvaluated(Long transactionId, int alertCount) {
        broadcast(
                "transaction-evaluated",
                new TransactionEvaluatedPayload(transactionId, alertCount)
        );
    }

    private void broadcast(String eventName, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException e) {
                log.debug("Removing stale SSE client: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }

    public record TransactionEvaluatedPayload(Long transactionId, int alertCount) {
    }
}
