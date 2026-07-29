package com.example.transactionmonitoring.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.RuleRepository;
import com.example.transactionmonitoring.rule.RuleChecker;

import lombok.RequiredArgsConstructor;

/**
 * Evaluates newly recorded transactions against every enabled monitoring rule.
 */
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final List<RuleChecker> ruleCheckers;
    private final RiskAccountService riskAccountService;

    @Transactional
    public List<Alert> evaluate(Transaction transaction) {
        List<Alert> triggeredAlerts = new ArrayList<>();
        List<Rule> triggeredRules = new ArrayList<>();

        for (Rule rule : ruleRepository.findByEnabledTrue()) {
            for (RuleChecker checker : ruleCheckers) {
                if (checker.check(transaction, rule)) {
                    triggeredAlerts.add(createAlert(transaction, rule));
                    triggeredRules.add(rule);
                    break;
                }
            }
        }

        if (triggeredAlerts.isEmpty()) {
            return List.of();
        }

        List<Alert> savedAlerts = alertRepository.saveAll(triggeredAlerts);
        for (int i = 0; i < savedAlerts.size(); i++) {
            riskAccountService.registerRisk(
                    savedAlerts.get(i),
                    transaction,
                    triggeredRules.get(i)
            );
        }
        return savedAlerts;
    }

    private Alert createAlert(Transaction transaction, Rule rule) {
        Alert alert = new Alert();
        alert.setTransactionId(transaction.getId());
        alert.setRuleId(rule.getId());
        alert.setRuleType(rule.getType());
        alert.setSeverity(rule.getSeverity());
        alert.setStatus(AlertStatus.OPEN);
        alert.setMessage(
                "Transaction %d triggered rule '%s' (%s)"
                        .formatted(transaction.getId(), rule.getName(), rule.getType())
        );
        return alert;
    }
}
