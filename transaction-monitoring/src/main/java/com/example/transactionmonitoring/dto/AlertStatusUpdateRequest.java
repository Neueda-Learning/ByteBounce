package com.example.transactionmonitoring.dto;

import com.example.transactionmonitoring.entity.AlertStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body used to move an alert to another lifecycle status.
 */
public record AlertStatusUpdateRequest(
        @NotNull(message = "status is required")
        AlertStatus status
) {
}
