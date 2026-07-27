package com.example.transactionmonitoring.dto;

import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Alert information combined with the transaction that triggered it.
 */
public record AlertDetailsResponse(
        Long alertId,
        Long ruleId,
        String ruleName,
        RuleType ruleType,
        Severity severity,
        AlertStatus status,
        String message,
        Long transactionId,
        String accountId,
        String payeeId,
        BigDecimal amount,
        String currency,
        String type,
        LocalDateTime transactionTime
) {
}
