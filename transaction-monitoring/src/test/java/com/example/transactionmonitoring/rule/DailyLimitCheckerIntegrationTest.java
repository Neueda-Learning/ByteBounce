package com.example.transactionmonitoring.rule;

import com.example.transactionmonitoring.dto.TransactionRequest;
import com.example.transactionmonitoring.dto.TransactionResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.RuleRepository;
import com.example.transactionmonitoring.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DailyLimitCheckerIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void configureOnlyDailyLimitRule() {
        ruleRepository.findAll().forEach(rule -> rule.setEnabled(false));
        ruleRepository.flush();

        Rule dailyLimitRule = new Rule();
        dailyLimitRule.setName("Daily Limit Integration Test");
        dailyLimitRule.setType(RuleType.DAILY_LIMIT);
        dailyLimitRule.setDescription("Daily limit checker integration test");
        dailyLimitRule.setThreshold(new BigDecimal("50000"));
        dailyLimitRule.setSeverity(Severity.HIGH);
        dailyLimitRule.setEnabled(true);
        ruleRepository.saveAndFlush(dailyLimitRule);
    }

    @Test
    void shouldGenerateAlertWhenDailyTotalExceedsThreshold() {
        String accountId = uniqueAccountId();

        createTransaction(accountId, "20000");
        createTransaction(accountId, "15000");
        TransactionResponse triggeringTransaction =
                createTransaction(accountId, "18000");

        assertThat(alertRepository.findAll())
                .filteredOn(alert -> isDailyLimitAlertFor(
                        alert,
                        triggeringTransaction.id()
                ))
                .hasSize(1);
    }

    @Test
    void shouldNotGenerateAlertWhenDailyTotalIsBelowThreshold() {
        String accountId = uniqueAccountId();

        TransactionResponse firstTransaction =
                createTransaction(accountId, "20000");
        TransactionResponse secondTransaction =
                createTransaction(accountId, "15000");
        Set<Long> testTransactionIds = Set.of(
                firstTransaction.id(),
                secondTransaction.id()
        );

        assertThat(alertRepository.findAll())
                .filteredOn(alert -> alert.getRuleType() == RuleType.DAILY_LIMIT
                        && testTransactionIds.contains(alert.getTransactionId()))
                .isEmpty();
    }

    private TransactionResponse createTransaction(
            String accountId,
            String amount
    ) {
        return transactionService.createTransaction(new TransactionRequest(
                accountId,
                "PAYEE-" + UUID.randomUUID(),
                new BigDecimal(amount),
                "USD",
                "DEBIT",
                "Daily limit integration test"
        ));
    }

    private boolean isDailyLimitAlertFor(Alert alert, Long transactionId) {
        return alert.getRuleType() == RuleType.DAILY_LIMIT
                && alert.getTransactionId().equals(transactionId);
    }

    private String uniqueAccountId() {
        return "DAILY-LIMIT-TEST-" + UUID.randomUUID();
    }
}
