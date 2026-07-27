package com.example.transactionmonitoring.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body used to enable or disable a monitoring rule.
 */
public record RuleStatusUpdateRequest(
        @NotNull(message = "enabled is required")
        Boolean enabled
) {
}
