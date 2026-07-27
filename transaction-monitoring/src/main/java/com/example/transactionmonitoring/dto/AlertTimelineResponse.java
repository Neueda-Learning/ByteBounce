package com.example.transactionmonitoring.dto;

import com.example.transactionmonitoring.entity.AlertStatus;

import java.time.LocalDateTime;

/**
 * One chronological alert lifecycle event.
 */
public record AlertTimelineResponse(
        AlertStatus status,
        LocalDateTime changedTime
) {
}
