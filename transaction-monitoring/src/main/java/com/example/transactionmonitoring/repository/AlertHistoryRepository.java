package com.example.transactionmonitoring.repository;

import com.example.transactionmonitoring.entity.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access operations for alert status history.
 */
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    Page<AlertHistory> findAllByOrderByChangedTimeDesc(Pageable pageable);

    List<AlertHistory> findByAlertIdOrderByChangedTimeAsc(Long alertId);
}
