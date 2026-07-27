package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.RuleRequest;
import com.example.transactionmonitoring.dto.RuleResponse;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.exception.InvalidRuleConfigurationException;
import com.example.transactionmonitoring.exception.ResourceNotFoundException;
import com.example.transactionmonitoring.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application service for managing configurable monitoring rules.
 */
@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public List<RuleResponse> getAllRules() {
        return ruleRepository.findAll()
                .stream()
                .map(RuleService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RuleResponse getRuleById(Long id) {
        return toResponse(findRule(id));
    }

    @Transactional
    public RuleResponse createRule(RuleRequest request) {
        validateRuleConfiguration(request);

        Rule rule = new Rule();
        applyRequest(rule, request);
        return toResponse(ruleRepository.saveAndFlush(rule));
    }

    @Transactional
    public RuleResponse updateRule(Long id, RuleRequest request) {
        validateRuleConfiguration(request);

        Rule rule = findRule(id);
        applyRequest(rule, request);
        return toResponse(ruleRepository.saveAndFlush(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        Rule rule = findRule(id);
        rule.setEnabled(false);
        ruleRepository.saveAndFlush(rule);
    }

    @Transactional
    public RuleResponse updateRuleStatus(Long id, boolean enabled) {
        Rule rule = findRule(id);
        rule.setEnabled(enabled);
        return toResponse(ruleRepository.saveAndFlush(rule));
    }

    private Rule findRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rule not found with id: " + id
                ));
    }

    private void validateRuleConfiguration(RuleRequest request) {
        switch (request.type()) {
            case AMOUNT_THRESHOLD, DAILY_LIMIT -> {
                if (request.threshold() == null
                        || request.threshold().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new InvalidRuleConfigurationException(
                            "threshold must be greater than zero for " + request.type()
                    );
                }
            }
            case VELOCITY -> {
                if (request.timeWindow() == null || request.timeWindow() <= 0
                        || request.maxCount() == null || request.maxCount() <= 0) {
                    throw new InvalidRuleConfigurationException(
                            "timeWindow and maxCount must be greater than zero for VELOCITY"
                    );
                }
            }
            case NEW_PAYEE -> {
                // New-payee detection does not require numeric parameters.
            }
        }
    }

    private static void applyRequest(Rule rule, RuleRequest request) {
        rule.setName(request.name().trim());
        rule.setType(request.type());
        rule.setDescription(request.description());
        rule.setThreshold(request.threshold());
        rule.setTimeWindow(request.timeWindow());
        rule.setMaxCount(request.maxCount());
        rule.setSeverity(request.severity());
        rule.setEnabled(request.enabled());
    }

    private static RuleResponse toResponse(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getType(),
                rule.getDescription(),
                rule.getThreshold(),
                rule.getTimeWindow(),
                rule.getMaxCount(),
                rule.getSeverity(),
                rule.isEnabled(),
                rule.getCreatedTime(),
                rule.getUpdatedTime()
        );
    }
}
