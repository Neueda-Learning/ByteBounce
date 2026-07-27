package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertDetailsResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.exception.ResourceNotFoundException;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.RuleRepository;
import com.example.transactionmonitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides an alert together with its triggering transaction.
 */
@Service
@RequiredArgsConstructor
public class AlertDetailsService {

    private final AlertRepository alertRepository;
    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public AlertDetailsResponse getAlertDetails(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alert not found with id: " + alertId
                ));

        Rule rule = ruleRepository.findById(alert.getRuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rule not found with id: " + alert.getRuleId()
                ));

        Transaction transaction = transactionRepository
                .findById(alert.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: "
                                + alert.getTransactionId()
                ));

        return new AlertDetailsResponse(
                alert.getId(),
                alert.getRuleId(),
                rule.getName(),
                alert.getRuleType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getMessage(),
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getPayeeId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getTransactionTime()
        );
    }
}
