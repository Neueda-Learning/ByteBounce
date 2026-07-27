package com.example.transactionmonitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction data returned by the REST API.
 */
public record TransactionResponse(
        Long id,
        String accountId,
        String payeeId,
        BigDecimal amount,
        String currency,
        String type,
        LocalDateTime transactionTime,
        String description,
        Boolean hasAlert,
        Integer alertCount
) {
}
