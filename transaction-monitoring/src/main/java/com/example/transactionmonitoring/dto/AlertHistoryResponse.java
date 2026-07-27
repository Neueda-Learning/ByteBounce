package com.example.transactionmonitoring.dto;

import com.example.transactionmonitoring.entity.AlertStatus;

import java.time.LocalDateTime;

/**
 * Audit information for one alert status transition.
 */
public record AlertHistoryResponse(
        Long id,
        Long alertId,
        AlertStatus oldStatus,
        AlertStatus newStatus,
        LocalDateTime changedTime
) {
}
