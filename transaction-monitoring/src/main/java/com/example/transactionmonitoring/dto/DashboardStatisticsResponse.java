package com.example.transactionmonitoring.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregate statistics displayed by the monitoring dashboard.
 */
public record DashboardStatisticsResponse(
        long totalTransactions,
        BigDecimal totalAmount,
        long totalAlerts,
        long openAlerts,
        String baseCurrency,
        Map<String, BigDecimal> exchangeRates
) {
}
