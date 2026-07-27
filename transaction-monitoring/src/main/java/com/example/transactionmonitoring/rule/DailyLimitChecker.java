package com.example.transactionmonitoring.rule;

import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Checks whether an account's cumulative transaction amount exceeds its daily limit.
 */
@Component
@RequiredArgsConstructor
public class DailyLimitChecker implements RuleChecker {

    private final TransactionRepository transactionRepository;

    @Override
    public boolean check(Transaction transaction, Rule rule) {
        if (rule.getType() != RuleType.DAILY_LIMIT
                || rule.getThreshold() == null
                || transaction.getTransactionTime() == null) {
            return false;
        }

        LocalDate transactionDate = transaction.getTransactionTime().toLocalDate();
        LocalDateTime dayStart = transactionDate.atStartOfDay();
        LocalDateTime dayEnd = transactionDate.atTime(LocalTime.MAX);

        BigDecimal dailyTotal =
                transactionRepository.findByAccountIdAndTransactionTimeBetween(
                                transaction.getAccountId(),
                                dayStart,
                                dayEnd
                        )
                        .stream()
                        .map(Transaction::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return dailyTotal.compareTo(rule.getThreshold()) > 0;
    }
}
