package com.example.transactionmonitoring.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transactionmonitoring.config.CurrencyConversionProperties;
import com.example.transactionmonitoring.dto.DashboardStatisticsResponse;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.exception.UnsupportedCurrencyException;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides read-only aggregate data for the monitoring dashboard.
 */
@Slf4j
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
                .map(transaction -> {
                    try {
                        return currencyConversionService.convertToBase(
                                transaction.getAmount(),
                                transaction.getCurrency()
                        );
                    } catch (UnsupportedCurrencyException exception) {
                        log.warn(
                                "Skipping transaction {} from dashboard total: {}",
                                transaction.getId(),
                                exception.getMessage()
                        );
                        return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "USD" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
