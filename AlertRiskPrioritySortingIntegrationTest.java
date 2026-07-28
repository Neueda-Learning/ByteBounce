package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertResponse;
import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Severity;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.RuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class AlertRiskPrioritySortingIntegrationTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Test
    void shouldSortByStatusThenSeverityThenNewestCreationTime() {
        Rule rule = createRule();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Alert closedHigh = createAlert(
                rule,
                AlertStatus.CLOSED,
                Severity.HIGH,
                now.plusMinutes(2)
        );
        Alert openMedium = createAlert(
                rule,
                AlertStatus.OPEN,
                Severity.MEDIUM,
                now.plusMinutes(1)
        );
        Alert openHigh = createAlert(
                rule,
                AlertStatus.OPEN,
                Severity.HIGH,
                now
        );
        alertRepository.saveAllAndFlush(
                List.of(closedHigh, openMedium, openHigh)
        );

        int resultSize = Math.toIntExact(alertRepository.count());
        PageResponse<AlertResponse> result =
                alertService.getAllAlerts(0, resultSize);
        List<Long> orderedIds = result.content()
                .stream()
                .map(AlertResponse::id)
                .toList();

        assertThat(orderedIds.indexOf(openHigh.getId()))
                .isLessThan(orderedIds.indexOf(openMedium.getId()));
        assertThat(orderedIds.indexOf(openMedium.getId()))
                .isLessThan(orderedIds.indexOf(closedHigh.getId()));
    }

    @Test
    void shouldKeepPaginationMetadataCorrect() {
        PageResponse<AlertResponse> firstPage =
                alertService.getAllAlerts(0, 10);

        assertThat(firstPage.page()).isZero();
        assertThat(firstPage.size()).isEqualTo(10);
        assertThat(firstPage.content()).hasSizeLessThanOrEqualTo(10);
        assertThat(firstPage.totalElements())
                .isEqualTo(alertRepository.count());
        assertThat(firstPage.totalPages())
                .isEqualTo((int) Math.ceil(firstPage.totalElements() / 10.0));
    }

    private Rule createRule() {
        Rule rule = new Rule();
        rule.setName("Risk Priority Test Rule " + UUID.randomUUID());
        rule.setType(RuleType.AMOUNT_THRESHOLD);
        rule.setDescription("Risk priority sorting integration test");
        rule.setSeverity(Severity.HIGH);
        rule.setEnabled(false);
        return ruleRepository.saveAndFlush(rule);
    }

    private Alert createAlert(
            Rule rule,
            AlertStatus status,
            Severity severity,
            LocalDateTime createdTime
    ) {
        Alert alert = new Alert();
        alert.setTransactionId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        alert.setRuleId(rule.getId());
        alert.setRuleType(rule.getType());
        alert.setSeverity(severity);
        alert.setStatus(status);
        alert.setMessage("Risk priority sorting integration test");
        alert.setCreatedTime(createdTime);
        return alert;
    }
}
