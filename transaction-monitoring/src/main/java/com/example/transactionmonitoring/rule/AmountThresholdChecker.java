package com.example.transactionmonitoring.rule;

import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Transaction;
import org.springframework.stereotype.Component;

/**
 * Checks whether a single transaction exceeds a configured amount threshold.
 */
@Component
public class AmountThresholdChecker implements RuleChecker {

    @Override
    public boolean check(Transaction transaction, Rule rule) {
        if (rule.getType() != RuleType.AMOUNT_THRESHOLD) {
            return false;
        }

        return transaction.getAmount() != null
                && rule.getThreshold() != null
                && transaction.getAmount().compareTo(rule.getThreshold()) > 0;
    }
}
