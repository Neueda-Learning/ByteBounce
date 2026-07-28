package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.config.CurrencyConversionProperties;
import com.example.transactionmonitoring.exception.UnsupportedCurrencyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * Converts transaction amounts into a configured base currency.
 */
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private final CurrencyConversionProperties properties;

    public BigDecimal convertToBase(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }

        String normalizedBaseCurrency = normalize(properties.getBase());
        String normalizedCurrency = currency == null
                ? normalizedBaseCurrency
                : normalize(currency);

        if (normalizedCurrency.equals(normalizedBaseCurrency)) {
            return amount;
        }

        BigDecimal rate = findRate(normalizedCurrency);
        return amount.multiply(rate);
    }

    private BigDecimal findRate(String currency) {
        Map<String, BigDecimal> rates = properties.getRates();
        BigDecimal rate = rates.get(currency);

        if (rate == null) {
            throw new UnsupportedCurrencyException(
                    "Unsupported currency for threshold evaluation: " + currency
            );
        }

        return rate;
    }

    private static String normalize(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}