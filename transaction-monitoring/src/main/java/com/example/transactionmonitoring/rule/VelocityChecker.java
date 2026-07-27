package com.example.transactionmonitoring.rule;

import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Checks the number of transactions made by an account in a rolling time window.
 */
@Component
@RequiredArgsConstructor
public class VelocityChecker implements RuleChecker {

    private final TransactionRepository transactionRepository;

    @Override
    public boolean check(Transaction transaction, Rule rule) {
        if (rule.getType() != RuleType.VELOCITY
                || transaction.getTransactionTime() == null
                || rule.getTimeWindow() == null
                || rule.getTimeWindow() <= 0
                || rule.getMaxCount() == null
                || rule.getMaxCount() < 0) {
            return false;
        }

        LocalDateTime windowStart = transaction.getTransactionTime()
                .minusMinutes(rule.getTimeWindow());
        long transactionCount =
                transactionRepository.countByAccountIdAndTransactionTimeBetween(
                        transaction.getAccountId(),
                        windowStart,
                        transaction.getTransactionTime()
                );

        return transactionCount > rule.getMaxCount();
    }
}
