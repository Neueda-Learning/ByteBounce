package com.example.transactionmonitoring.messaging;

import com.example.transactionmonitoring.config.RabbitMQConfig;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.exception.ResourceNotFoundException;
import com.example.transactionmonitoring.repository.TransactionRepository;
import com.example.transactionmonitoring.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link TransactionCreatedEvent} messages from RabbitMQ and
 * triggers rule evaluation for the referenced transaction, decoupling
 * evaluation from the transaction-creation request.
 */
@Component
@RequiredArgsConstructor
public class TransactionCreatedListener {

    private final TransactionRepository transactionRepository;
    private final RuleEngineService ruleEngineService;

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_CREATED_QUEUE)
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        Transaction transaction = transactionRepository.findById(event.transactionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + event.transactionId()
                ));
        ruleEngineService.evaluate(transaction);
    }
}
