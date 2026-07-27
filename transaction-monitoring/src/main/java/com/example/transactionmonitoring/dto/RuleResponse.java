package com.example.transactionmonitoring.dto;

import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Monitoring rule data returned by the REST API.
 */
public record RuleResponse(
        Long id,
        String name,
        RuleType type,
        String description,
        BigDecimal threshold,
        Integer timeWindow,
        Integer maxCount,
        Severity severity,
        boolean enabled,
        LocalDateTime createdTime,
        LocalDateTime updatedTime
) {
}
