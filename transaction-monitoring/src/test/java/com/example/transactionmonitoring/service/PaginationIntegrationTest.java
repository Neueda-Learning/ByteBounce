package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertHistoryResponse;
import com.example.transactionmonitoring.dto.AlertResponse;
import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.dto.TransactionResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.AlertHistory;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.repository.AlertHistoryRepository;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.RuleRepository;
import com.example.transactionmonitoring.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class PaginationIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertHistoryService alertHistoryService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertHistoryRepository alertHistoryRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Test
    void shouldReturnFirstTransactionPageWithCorrectTotal() {
        createTransactions("PAGE-ALL-" + UUID.randomUUID(), 12);

        PageResponse<TransactionResponse> response =
                transactionService.getAllTransactions(0, 10);

        assertThat(response.content()).hasSize(10);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(transactionRepository.count());
    }

    @Test
    void shouldReturnFirstAlertPageWithCorrectTotal() {
        Rule rule = createRule();
        Transaction transaction = createTransaction(
                "ALERT-PAGE-" + UUID.randomUUID(),
                0
        );
        for (int index = 0; index < 12; index++) {
            createAlert(rule, transaction, Severity.HIGH);
        }
        alertRepository.flush();

        PageResponse<AlertResponse> response = alertService.getAllAlerts(0, 10);

        assertThat(response.content()).hasSize(10);
        assertThat(response.totalElements()).isEqualTo(alertRepository.count());
    }

    @Test
    void shouldCombineTransactionSearchAndPagination() {
        String accountId = "PAGE-SEARCH-" + UUID.randomUUID();
        createTransactions(accountId, 13);

        PageResponse<TransactionResponse> response =
                transactionService.searchTransactions(
                        accountId,
                        null,
                        "DEBIT",
                        new BigDecimal("10"),
                        new BigDecimal("100"),
                        1,
                        5
                );

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.content()).hasSize(5);
        assertThat(response.totalElements()).isEqualTo(13);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.content())
                .allMatch(item -> item.accountId().equals(accountId));
    }

    @Test
    void shouldPageAlertHistoryInDescendingTimeOrder() {
        LocalDateTime baseTime = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
        for (int index = 0; index < 12; index++) {
            AlertHistory history = new AlertHistory();
            history.setAlertId(100_000L + index);
            history.setOldStatus(AlertStatus.OPEN);
            history.setNewStatus(AlertStatus.ACKNOWLEDGED);
            history.setChangedTime(baseTime.plusMinutes(index));
            alertHistoryRepository.save(history);
        }
        alertHistoryRepository.flush();

        PageResponse<AlertHistoryResponse> response =
                alertHistoryService.getAllHistory(0, 10);

        assertThat(response.content()).hasSize(10);
        assertThat(response.totalElements()).isEqualTo(alertHistoryRepository.count());
        assertThat(response.content())
                .extracting(AlertHistoryResponse::changedTime)
                .isSortedAccordingTo((left, right) -> right.compareTo(left));
    }

    private void createTransactions(String accountId, int count) {
        for (int index = 0; index < count; index++) {
            createTransaction(accountId, index);
        }
        transactionRepository.flush();
    }

    private Transaction createTransaction(String accountId, int index) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(accountId);
        transaction.setPayeeId("PAGE-PAYEE-" + index);
        transaction.setAmount(new BigDecimal("50"));
        transaction.setCurrency("USD");
        transaction.setType("DEBIT");
        transaction.setTransactionTime(
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(index)
        );
        transaction.setDescription("Pagination integration test");
        return transactionRepository.save(transaction);
    }

    private Rule createRule() {
        Rule rule = new Rule();
        rule.setName("Pagination Rule " + UUID.randomUUID());
        rule.setType(RuleType.AMOUNT_THRESHOLD);
        rule.setDescription("Pagination integration test");
        rule.setThreshold(new BigDecimal("100"));
        rule.setSeverity(Severity.HIGH);
        rule.setEnabled(false);
        return ruleRepository.saveAndFlush(rule);
    }

    private void createAlert(
            Rule rule,
            Transaction transaction,
            Severity severity
    ) {
        Alert alert = new Alert();
        alert.setTransactionId(transaction.getId());
        alert.setRuleId(rule.getId());
        alert.setRuleType(rule.getType());
        alert.setSeverity(severity);
        alert.setStatus(AlertStatus.OPEN);
        alert.setMessage("Pagination integration test");
        alertRepository.save(alert);
    }
}
