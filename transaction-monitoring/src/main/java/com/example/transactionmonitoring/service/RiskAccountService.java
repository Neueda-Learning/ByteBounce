package com.example.transactionmonitoring.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.RiskAccount;
import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.Severity;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.exception.AccountSuspendedException;
import com.example.transactionmonitoring.exception.TransactionLimitExceededException;
import com.example.transactionmonitoring.repository.RiskAccountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Manages the risk status of accounts based on the alerts they trigger, and
 * enforces the resulting restrictions when new transactions are created.
 */
@Service
@RequiredArgsConstructor
public class RiskAccountService {

    private static final BigDecimal HALF = new BigDecimal("0.5");

    /**
     * Fallback base limit (in base currency) used for MEDIUM risk when the
     * triggering rule has no numeric threshold to derive a limit from
     * (e.g. VELOCITY, NEW_PAYEE).
     */
    private static final BigDecimal DEFAULT_BASE_LIMIT = new BigDecimal("5000");

    private final RiskAccountRepository riskAccountRepository;
    private final CurrencyConversionService currencyConversionService;

    /**
     * Flags both the payer and payee of a transaction that triggered a
     * HIGH or MEDIUM severity alert. LOW severity alerts do not affect risk status.
     */
    @Transactional
    public void registerRisk(Alert alert, Transaction transaction, Rule rule) {
        Severity riskLevel = rule.getSeverity();
        if (riskLevel != Severity.HIGH && riskLevel != Severity.MEDIUM) {
            return;
        }

        BigDecimal transactionLimit = null;
        if (riskLevel == Severity.MEDIUM) {
            BigDecimal baseLimit = rule.getThreshold() != null
                    ? rule.getThreshold()
                    : DEFAULT_BASE_LIMIT;
            transactionLimit = baseLimit.multiply(HALF);
        }

        flagAccount(transaction.getAccountId(), alert.getId(), riskLevel, transactionLimit);
        flagAccount(transaction.getPayeeId(), alert.getId(), riskLevel, transactionLimit);
    }

    private void flagAccount(
            String accountId,
            Long alertId,
            Severity riskLevel,
            BigDecimal transactionLimit
    ) {
        if (accountId == null || accountId.isBlank()) {
            return;
        }
        if (riskAccountRepository.existsByAccountIdAndAlertId(accountId, alertId)) {
            return;
        }

        RiskAccount riskAccount = new RiskAccount();
        riskAccount.setAccountId(accountId);
        riskAccount.setAlertId(alertId);
        riskAccount.setRiskLevel(riskLevel);
        riskAccount.setTransactionLimit(transactionLimit);
        riskAccountRepository.save(riskAccount);
    }

    /**
     * Removes the risk flags tied to an alert once it has been dismissed.
     */
    @Transactional
    public void releaseRisk(Long alertId) {
        riskAccountRepository.deleteByAlertId(alertId);
    }

    /**
     * Validates a new transaction against the risk status of the payer and
     * payee accounts. Throws if either account is suspended (HIGH risk) or if
     * the amount exceeds a medium-risk account's restricted limit.
     */
    @Transactional(readOnly = true)
    public void assertTransactionAllowed(
            String accountId,
            String payeeId,
            BigDecimal amount,
            String currency
    ) {
        List<RiskAccount> payerRisks = riskAccountRepository.findByAccountId(accountId);
        List<RiskAccount> payeeRisks = payeeId == null || payeeId.isBlank()
                ? List.of()
                : riskAccountRepository.findByAccountId(payeeId);

        assertNotSuspended(payerRisks, accountId);
        assertNotSuspended(payeeRisks, payeeId);

        BigDecimal baseAmount = currencyConversionService.convertToBase(amount, currency);
        assertWithinLimit(payerRisks, accountId, baseAmount);
        assertWithinLimit(payeeRisks, payeeId, baseAmount);
    }

    private void assertNotSuspended(List<RiskAccount> risks, String accountId) {
        boolean suspended = risks.stream()
                .anyMatch(risk -> risk.getRiskLevel() == Severity.HIGH);
        if (suspended) {
            throw new AccountSuspendedException(
                    "Account " + accountId + " is suspended due to high risk and cannot transact"
            );
        }
    }

    private void assertWithinLimit(List<RiskAccount> risks, String accountId, BigDecimal amount) {
        BigDecimal strictestLimit = risks.stream()
                .filter(risk -> risk.getRiskLevel() == Severity.MEDIUM)
                .map(risk -> risk.getTransactionLimit() != null
                        ? risk.getTransactionLimit()
                        : DEFAULT_BASE_LIMIT.multiply(HALF))
                .min(BigDecimal::compareTo)
                .orElse(null);

        if (strictestLimit != null && amount != null && amount.compareTo(strictestLimit) > 0) {
            throw new TransactionLimitExceededException(
                    "Account " + accountId + " exceeds its restricted transaction limit of " + strictestLimit
            );
        }
    }
}
