package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.dto.AlertResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.AlertHistory;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.Severity;
import com.example.transactionmonitoring.exception.InvalidAlertStatusTransitionException;
import com.example.transactionmonitoring.exception.ResourceNotFoundException;
import com.example.transactionmonitoring.repository.AlertHistoryRepository;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Application service for querying alerts and managing their lifecycle.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final Map<AlertStatus, Set<AlertStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    AlertStatus.OPEN, Set.of(AlertStatus.ACKNOWLEDGED),
                    AlertStatus.ACKNOWLEDGED, Set.of(
                            AlertStatus.INVESTIGATING,
                            AlertStatus.DISMISSED
                    ),
                    AlertStatus.INVESTIGATING, Set.of(
                            AlertStatus.CLOSED,
                            AlertStatus.DISMISSED
                    ),
                    AlertStatus.CLOSED, Set.of(),
                    AlertStatus.DISMISSED, Set.of()
            );

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final RuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> getAllAlerts(int page, int size) {
        Page<Alert> alerts = alertRepository.findAllByRiskPriority(
                pageRequest(page, size)
        );
        return toPageResponse(alerts);
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long id) {
        Alert alert = findAlert(id);
        return toResponse(alert, findRule(alert));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> searchAlerts(
            Severity severity,
            AlertStatus status,
            Long ruleId,
            String ruleName
    ) {
        return searchAlerts(
                severity,
                status,
                ruleId,
                ruleName,
                0,
                Integer.MAX_VALUE
        ).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> searchAlerts(
            Severity severity,
            AlertStatus status,
            Long ruleId,
            String ruleName,
            int page,
            int size
    ) {
        Page<Alert> alerts = alertRepository.search(
                        severity,
                        status,
                        ruleId,
                        normalize(ruleName),
                        pageRequest(page, size)
                );
        Page<AlertResponse> responsePage = alerts
                .map(alert -> toResponse(alert, findRule(alert)));
        return PageResponse.from(responsePage);
    }

    private PageResponse<AlertResponse> toPageResponse(Page<Alert> alerts) {
        Page<AlertResponse> responsePage = alerts
                .map(alert -> toResponse(alert, findRule(alert)));
        return PageResponse.from(responsePage);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1)
        );
    }

    @Transactional
    public AlertResponse updateAlertStatus(Long id, AlertStatus newStatus) {
        Alert alert = findAlert(id);
        AlertStatus oldStatus = alert.getStatus();
        validateTransition(oldStatus, newStatus);

        LocalDateTime changedTime = LocalDateTime.now(ZoneOffset.UTC);
        alert.setStatus(newStatus);
        alert.setUpdatedTime(changedTime);
        Alert savedAlert = alertRepository.save(alert);

        AlertHistory history = new AlertHistory();
        history.setAlertId(savedAlert.getId());
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedTime(changedTime);
        alertHistoryRepository.save(history);

        return toResponse(savedAlert, findRule(savedAlert));
    }

    private Alert findAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alert not found with id: " + id
                ));
    }

    private Rule findRule(Alert alert) {
        return ruleRepository.findById(alert.getRuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rule not found with id: " + alert.getRuleId()
                ));
    }

    private void validateTransition(AlertStatus oldStatus, AlertStatus newStatus) {
        if (oldStatus == null
                || newStatus == null
                || !ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of())
                .contains(newStatus)) {
            throw new InvalidAlertStatusTransitionException(
                    "Alert status cannot change from %s to %s"
                            .formatted(oldStatus, newStatus)
            );
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static AlertResponse toResponse(Alert alert, Rule rule) {
        return new AlertResponse(
                alert.getId(),
                alert.getTransactionId(),
                alert.getRuleId(),
                rule.getName(),
                alert.getRuleType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getMessage(),
                alert.getCreatedTime(),
                alert.getUpdatedTime()
        );
    }
}
