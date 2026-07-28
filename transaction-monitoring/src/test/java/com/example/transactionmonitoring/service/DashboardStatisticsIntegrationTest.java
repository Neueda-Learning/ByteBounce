package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.DashboardStatisticsResponse;
import com.example.transactionmonitoring.dto.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DashboardStatisticsIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TransactionService transactionService;

    @Test
    void shouldAggregateDashboardAmountUsingBaseCurrencyExchangeRates() {
        DashboardStatisticsResponse baseline = dashboardService.getStatistics();

        createTransaction("100.00", "USD");
        createTransaction("100.00", "CNY");
        createTransaction("100.00", "EUR");

        DashboardStatisticsResponse updated = dashboardService.getStatistics();

        assertThat(updated.totalTransactions() - baseline.totalTransactions())
                .isEqualTo(3);
        assertThat(updated.totalAmount().subtract(baseline.totalAmount()))
                .isEqualByComparingTo(new BigDecimal("231.00"));
        assertThat(updated.baseCurrency()).isEqualTo("USD");
        assertThat(updated.exchangeRates())
                .containsAllEntriesOf(Map.of(
                        "USD", new BigDecimal("1"),
                        "CNY", new BigDecimal("0.14"),
                        "EUR", new BigDecimal("1.17")
                ));
    }

    private void createTransaction(String amount, String currency) {
        String suffix = UUID.randomUUID().toString();

        transactionService.createTransaction(new TransactionRequest(
                "DASHBOARD-ACCOUNT-" + suffix,
                "DASHBOARD-PAYEE-" + suffix,
                new BigDecimal(amount),
                currency,
                "DEBIT",
                "Dashboard statistics integration test"
        ));
    }
}