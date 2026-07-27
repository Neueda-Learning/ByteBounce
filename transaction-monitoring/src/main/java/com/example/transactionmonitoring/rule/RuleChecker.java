package com.example.transactionmonitoring.rule;

import com.example.transactionmonitoring.entity.Rule;
import com.example.transactionmonitoring.entity.Transaction;

/**
 * Contract implemented by each transaction-monitoring rule checker.
 */
public interface RuleChecker {

    boolean check(Transaction transaction, Rule rule);
}
