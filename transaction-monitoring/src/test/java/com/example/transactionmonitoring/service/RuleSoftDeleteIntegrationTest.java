package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertResponse;
import com.example.transactionmonitoring.dto.RuleResponse;
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
class RuleSoftDeleteIntegrationTest {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AlertService alertService;

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
    void shouldPreserveRuleAssociationAfterRuleDeletion() {
        Rule rule = createAmountRule();
        TransactionResponse transaction = createTriggeredTransaction();
        Alert alert = alertRepository.findAll()
                .stream()
                .filter(item -> item.getTransactionId().equals(transaction.id()))
                .findFirst()
                .orElseThrow();

        ruleService.deleteRule(rule.getId());

        AlertResponse alertResponse = alertService.getAlertById(alert.getId());
        RuleResponse ruleResponse = ruleService.getRuleById(rule.getId());

        assertThat(alertResponse.ruleId()).isEqualTo(rule.getId());
        assertThat(alertResponse.ruleName()).isEqualTo(rule.getName());
        assertThat(alertResponse.ruleType()).isEqualTo(RuleType.AMOUNT_THRESHOLD);
        assertThat(ruleResponse.enabled()).isFalse();
        assertThat(ruleRepository.existsById(rule.getId())).isTrue();
    }

    private Rule createAmountRule() {
        Rule rule = new Rule();
        rule.setName("Soft Delete Rule " + UUID.randomUUID());
        rule.setType(RuleType.AMOUNT_THRESHOLD);
        rule.setDescription("Rule soft-delete integration test");
        rule.setThreshold(new BigDecimal("100"));
        rule.setSeverity(Severity.HIGH);
        rule.setEnabled(true);
        return ruleRepository.saveAndFlush(rule);
    }

    private TransactionResponse createTriggeredTransaction() {
        String uniqueId = UUID.randomUUID().toString();
        return transactionService.createTransaction(new TransactionRequest(
                "SOFT-DELETE-ACCOUNT-" + uniqueId,
                "SOFT-DELETE-PAYEE-" + uniqueId,
                new BigDecimal("150"),
                "USD",
                "DEBIT",
                "Rule soft-delete integration test"
        ));
    }
}
