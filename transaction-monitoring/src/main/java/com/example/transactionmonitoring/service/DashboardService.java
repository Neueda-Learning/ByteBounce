package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.config.CurrencyConversionProperties;
import com.example.transactionmonitoring.dto.DashboardStatisticsResponse;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provides read-only aggregate data for the monitoring dashboard.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final CurrencyConversionService currencyConversionService;
    private final CurrencyConversionProperties currencyConversionProperties;

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getStatistics() {
        return new DashboardStatisticsResponse(
                transactionRepository.count(),
                calculateTotalAmountInBaseCurrency(),
                alertRepository.count(),
                alertRepository.countByStatus(AlertStatus.OPEN),
                normalizeCurrency(currencyConversionProperties.getBase()),
                new LinkedHashMap<>(currencyConversionProperties.getRates())
        );
    }

    private BigDecimal calculateTotalAmountInBaseCurrency() {
        return transactionRepository.findAll()
                .stream()
                .map(transaction -> currencyConversionService.convertToBase(
                        transaction.getAmount(),
                        transaction.getCurrency()
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "USD" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
