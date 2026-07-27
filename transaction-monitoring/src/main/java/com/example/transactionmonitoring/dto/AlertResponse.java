package com.example.transactionmonitoring.dto;

import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;

import java.time.LocalDateTime;

/**
 * Alert data returned by the REST API.
 */
public record AlertResponse(
        Long id,
        Long transactionId,
        Long ruleId,
        String ruleName,
        RuleType ruleType,
        Severity severity,
        AlertStatus status,
        String message,
        LocalDateTime createdTime,
        LocalDateTime updatedTime
) {
}
