package com.example.transactionmonitoring.dto;

import java.math.BigDecimal;

/**
 * Aggregate statistics displayed by the monitoring dashboard.
 */
public record DashboardStatisticsResponse(
        long totalTransactions,
        BigDecimal totalAmount,
        long totalAlerts,
        long openAlerts
) {
}
