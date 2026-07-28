package com.example.transactionmonitoring.rule;

import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Checks whether a single transaction exceeds a configured amount threshold.
 */
@Component
@RequiredArgsConstructor
public class AmountThresholdChecker implements RuleChecker {

    private final CurrencyConversionService currencyConversionService;

    @Override
    public boolean check(Transaction transaction, Rule rule) {
        if (rule.getType() != RuleType.AMOUNT_THRESHOLD) {
            return false;
        }

        return transaction.getAmount() != null
                && rule.getThreshold() != null
                && currencyConversionService
                .convertToBase(transaction.getAmount(), transaction.getCurrency())
                .compareTo(rule.getThreshold()) > 0;
    }
}
