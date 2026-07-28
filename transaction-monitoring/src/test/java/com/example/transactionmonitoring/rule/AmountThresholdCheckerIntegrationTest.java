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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class AmountThresholdCheckerIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void configureOnlyAmountThresholdRule() {
        ruleRepository.findAll().forEach(rule -> rule.setEnabled(false));
        ruleRepository.flush();

        Rule amountRule = new Rule();
        amountRule.setName("Amount Threshold Integration Test");
        amountRule.setType(RuleType.AMOUNT_THRESHOLD);
        amountRule.setDescription("Amount threshold checker integration test");
        amountRule.setThreshold(new BigDecimal("1000"));
        amountRule.setSeverity(Severity.HIGH);
        amountRule.setEnabled(true);
        ruleRepository.saveAndFlush(amountRule);
    }

    @Test
    void shouldGenerateAlertAfterConvertingTransactionAmountToBaseCurrency() {
        TransactionResponse triggeringTransaction = createTransaction(
                new BigDecimal("8000"),
                "CNY"
        );

        assertThat(alertRepository.findAll())
                .filteredOn(alert -> isAmountThresholdAlertFor(
                        alert,
                        triggeringTransaction.id()
                ))
                .hasSize(1);
    }

    @Test
    void shouldNotGenerateAlertWhenConvertedAmountStaysBelowThreshold() {
        TransactionResponse transaction = createTransaction(
                new BigDecimal("6000"),
                "CNY"
        );

        assertThat(alertRepository.findAll())
                .filteredOn(alert -> isAmountThresholdAlertFor(
                        alert,
                        transaction.id()
                ))
                .isEmpty();
    }

    private TransactionResponse createTransaction(BigDecimal amount, String currency) {
        String uniqueId = UUID.randomUUID().toString();
        return transactionService.createTransaction(new TransactionRequest(
                "AMOUNT-THRESHOLD-ACCOUNT-" + uniqueId,
                "AMOUNT-THRESHOLD-PAYEE-" + uniqueId,
                amount,
                currency,
                "DEBIT",
                "Amount threshold integration test"
        ));
    }

    private boolean isAmountThresholdAlertFor(Alert alert, Long transactionId) {
        return alert.getRuleType() == RuleType.AMOUNT_THRESHOLD
                && alert.getTransactionId().equals(transactionId);
    }
}