package com.example.transactionmonitoring.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.transactionmonitoring.entity.RiskAccount;

/**
 * Data access operations for risk-flagged accounts.
 */
public interface RiskAccountRepository extends JpaRepository<RiskAccount, Long> {

    List<RiskAccount> findByAccountId(String accountId);

    boolean existsByAccountIdAndAlertId(String accountId, Long alertId);

    void deleteByAlertId(Long alertId);
}
