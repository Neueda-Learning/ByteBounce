package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertDetailsResponse;
import com.example.transactionmonitoring.dto.AlertResponse;
import com.example.transactionmonitoring.dto.TransactionRequest;
import com.example.transactionmonitoring.dto.TransactionResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.RuleRepository;
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
class AlertRuleAssociationIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertDetailsService alertDetailsService;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void disableExistingRules() {
        ruleRepository.findAll().forEach(rule -> rule.setEnabled(false));
        ruleRepository.flush();
    }

    @Test
    void shouldAssociateAmountThresholdAlertWithExactRule() {
        Rule rule = createRule(
                "Association Amount Rule",
                RuleType.AMOUNT_THRESHOLD,
                new BigDecimal("1000"),
                Severity.HIGH
        );

        TransactionResponse transaction = createTransaction(
                "1500",
                "Amount rule association test"
        );
        Alert alert = findAlert(transaction.id(), RuleType.AMOUNT_THRESHOLD);

        assertThat(alert.getRuleId()).isEqualTo(rule.getId());

        AlertResponse response = alertService.getAlertById(alert.getId());
        assertThat(response.ruleId()).isEqualTo(rule.getId());
        assertThat(response.ruleName()).isEqualTo(rule.getName());
    }

    @Test
    void shouldAssociateNewPayeeAlertAndExposeRuleInDetails() {
        Rule rule = createRule(
                "Association New Payee Rule",
                RuleType.NEW_PAYEE,
                null,
                Severity.MEDIUM
        );

        TransactionResponse transaction = createTransaction(
                "25",
                "New payee rule association test"
        );
        Alert alert = findAlert(transaction.id(), RuleType.NEW_PAYEE);

        assertThat(alert.getRuleId()).isEqualTo(rule.getId());

        AlertDetailsResponse details =
                alertDetailsService.getAlertDetails(alert.getId());
        assertThat(details.ruleId()).isEqualTo(rule.getId());
        assertThat(details.ruleName()).isEqualTo(rule.getName());
        assertThat(details.ruleType()).isEqualTo(RuleType.NEW_PAYEE);
    }

    private Rule createRule(
            String name,
            RuleType type,
            BigDecimal threshold,
            Severity severity
    ) {
        Rule rule = new Rule();
        rule.setName(name);
        rule.setType(type);
        rule.setDescription("Alert-rule association integration test");
        rule.setThreshold(threshold);
        rule.setSeverity(severity);
        rule.setEnabled(true);
        return ruleRepository.saveAndFlush(rule);
    }

    private TransactionResponse createTransaction(
            String amount,
            String description
    ) {
        String uniqueId = UUID.randomUUID().toString();
        return transactionService.createTransaction(new TransactionRequest(
                "RULE-ASSOCIATION-ACCOUNT-" + uniqueId,
                "RULE-ASSOCIATION-PAYEE-" + uniqueId,
                new BigDecimal(amount),
                "USD",
                "DEBIT",
                description
        ));
    }

    private Alert findAlert(Long transactionId, RuleType ruleType) {
        return alertRepository.findAll()
                .stream()
                .filter(alert -> alert.getTransactionId().equals(transactionId))
                .filter(alert -> alert.getRuleType() == ruleType)
                .findFirst()
                .orElseThrow();
    }
}
