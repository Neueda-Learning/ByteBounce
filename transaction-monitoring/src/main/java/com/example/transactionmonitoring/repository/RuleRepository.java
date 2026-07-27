package com.example.transactionmonitoring.repository;

import com.example.transactionmonitoring.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access operations for monitoring rules.
 */
public interface RuleRepository extends JpaRepository<Rule, Long> {

    List<Rule> findByEnabledTrue();
}
