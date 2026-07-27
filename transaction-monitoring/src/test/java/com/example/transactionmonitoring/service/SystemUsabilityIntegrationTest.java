package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertResponse;
import com.example.transactionmonitoring.dto.AlertTimelineResponse;
import com.example.transactionmonitoring.dto.TransactionRequest;
import com.example.transactionmonitoring.dto.TransactionResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.AlertStatus;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SystemUsabilityIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertTimelineService alertTimelineService;

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
    void shouldFilterTransactionsByAccount() {
        String accountId = "SEARCH-ACCOUNT-" + UUID.randomUUID();
        TransactionResponse expected = createTransaction(
                accountId,
                "SEARCH-PAYEE-" + UUID.randomUUID(),
                "250",
                "DEBIT"
        );

        List<TransactionResponse> result = transactionService.searchTransactions(
                accountId,
                null,
                null,
                null,
                null
        );

        assertThat(result).extracting(TransactionResponse::id)
                .contains(expected.id());
        assertThat(result).allMatch(item -> item.accountId().equals(accountId));
    }

    @Test
    void shouldExposeAlertRiskIndicator() {
        createAmountRule("Risk Indicator Rule", "100", Severity.HIGH);

        TransactionResponse result = createTransaction(
                "RISK-ACCOUNT-" + UUID.randomUUID(),
                "RISK-PAYEE-" + UUID.randomUUID(),
                "150",
                "DEBIT"
        );

        assertThat(result.hasAlert()).isTrue();
        assertThat(result.alertCount()).isEqualTo(1);
        assertThat(transactionService.getTransactionById(result.id()).hasAlert())
                .isTrue();
    }

    @Test
    void shouldFilterAlertsBySeverityStatusAndRuleName() {
        Rule rule = createAmountRule(
                "High Risk Search Rule " + UUID.randomUUID(),
                "100",
                Severity.HIGH
        );
        TransactionResponse transaction = createTransaction(
                "ALERT-SEARCH-ACCOUNT-" + UUID.randomUUID(),
                "ALERT-SEARCH-PAYEE-" + UUID.randomUUID(),
                "150",
                "DEBIT"
        );

        List<AlertResponse> result = alertService.searchAlerts(
                Severity.HIGH,
                AlertStatus.OPEN,
                null,
                "Risk Search"
        );

        assertThat(result).anySatisfy(alert -> {
            assertThat(alert.transactionId()).isEqualTo(transaction.id());
            assertThat(alert.ruleId()).isEqualTo(rule.getId());
            assertThat(alert.severity()).isEqualTo(Severity.HIGH);
            assertThat(alert.status()).isEqualTo(AlertStatus.OPEN);
        });
    }

    @Test
    void shouldReturnAlertTimelineInChronologicalOrder() {
        createAmountRule("Timeline Rule", "100", Severity.MEDIUM);
        TransactionResponse transaction = createTransaction(
                "TIMELINE-ACCOUNT-" + UUID.randomUUID(),
                "TIMELINE-PAYEE-" + UUID.randomUUID(),
                "150",
                "DEBIT"
        );
        Alert alert = alertRepository.findAll()
                .stream()
                .filter(item -> item.getTransactionId().equals(transaction.id()))
                .findFirst()
                .orElseThrow();

        alertService.updateAlertStatus(alert.getId(), AlertStatus.ACKNOWLEDGED);
        alertService.updateAlertStatus(alert.getId(), AlertStatus.INVESTIGATING);

        List<AlertTimelineResponse> timeline =
                alertTimelineService.getTimeline(alert.getId());

        assertThat(timeline)
                .extracting(AlertTimelineResponse::status)
                .containsExactly(
                        AlertStatus.OPEN,
                        AlertStatus.ACKNOWLEDGED,
                        AlertStatus.INVESTIGATING
                );
        assertThat(timeline)
                .extracting(AlertTimelineResponse::changedTime)
                .isSorted();
    }

    private Rule createAmountRule(
            String name,
            String threshold,
            Severity severity
    ) {
        Rule rule = new Rule();
        rule.setName(name);
        rule.setType(RuleType.AMOUNT_THRESHOLD);
        rule.setDescription("System usability integration test");
        rule.setThreshold(new BigDecimal(threshold));
        rule.setSeverity(severity);
        rule.setEnabled(true);
        return ruleRepository.saveAndFlush(rule);
    }

    private TransactionResponse createTransaction(
            String accountId,
            String payeeId,
            String amount,
            String type
    ) {
        return transactionService.createTransaction(new TransactionRequest(
                accountId,
                payeeId,
                new BigDecimal(amount),
                "USD",
                type,
                "System usability integration test"
        ));
    }
}
