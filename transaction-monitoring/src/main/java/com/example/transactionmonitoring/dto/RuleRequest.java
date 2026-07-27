package com.example.transactionmonitoring.dto;

import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body used to create or replace a monitoring rule.
 */
public record RuleRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,

        @NotNull(message = "type is required")
        RuleType type,

        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        BigDecimal threshold,

        Integer timeWindow,

        Integer maxCount,

        @NotNull(message = "severity is required")
        Severity severity,

        @NotNull(message = "enabled is required")
        Boolean enabled
) {
}
