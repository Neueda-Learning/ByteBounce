package com.example.transactionmonitoring.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body used to create a transaction.
 */
public record TransactionRequest(
        @NotBlank(message = "accountId is required")
        @Size(max = 64, message = "accountId must not exceed 64 characters")
        String accountId,

        @NotBlank(message = "payeeId is required")
        @Size(max = 64, message = "payeeId must not exceed 64 characters")
        String payeeId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter code")
        String currency,

        @NotBlank(message = "type is required")
        @Size(max = 32, message = "type must not exceed 32 characters")
        String type,

        @Size(max = 500, message = "description must not exceed 500 characters")
        String description
) {
}
