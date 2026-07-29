package com.example.transactionmonitoring.config;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configures the base currency and exchange rates used by amount-based rules.
 */
@Component
@ConfigurationProperties(prefix = "monitoring.currency")
@Getter
@Setter
public class CurrencyConversionProperties {

    private String base = "USD";

    private Map<String, BigDecimal> rates = new LinkedHashMap<>(Map.of(
            "USD", BigDecimal.ONE,
            "CNY", new BigDecimal("0.14"),
            "EUR", new BigDecimal("1.17"),
            "GBP", new BigDecimal("1.27")
    ));
}