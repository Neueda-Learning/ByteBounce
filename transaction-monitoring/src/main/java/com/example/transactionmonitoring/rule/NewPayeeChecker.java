package com.example.transactionmonitoring.rule;

import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.RuleType;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Checks whether an account is sending to a payee it has not used before.
 */
@Component
@RequiredArgsConstructor
public class NewPayeeChecker implements RuleChecker {

    private final TransactionRepository transactionRepository;

    @Override
    public boolean check(Transaction transaction, Rule rule) {
        if (rule.getType() != RuleType.NEW_PAYEE || transaction.getId() == null) {
            return false;
        }

        return !transactionRepository.existsByAccountIdAndPayeeIdAndIdNot(
                transaction.getAccountId(),
                transaction.getPayeeId(),
                transaction.getId()
        );
    }
}
