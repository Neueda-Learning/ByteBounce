package com.example.transactionmonitoring.repository;

import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Data access operations for alerts.
 */
public interface AlertRepository extends JpaRepository<Alert, Long> {

    long countByStatus(AlertStatus status);

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findBySeverity(Severity severity);

    long countByTransactionId(Long transactionId);

    @Query("""
            select a.transactionId as transactionId, count(a.id) as alertCount
            from Alert a
            where a.transactionId in :transactionIds
            group by a.transactionId
            """)
    List<TransactionAlertCount> countByTransactionIds(
            @Param("transactionIds") Collection<Long> transactionIds
    );

    @Query("""
            select a
            from Alert a
            join Rule r on r.id = a.ruleId
            where (:severity is null or a.severity = :severity)
              and (:status is null or a.status = :status)
              and (:ruleId is null or a.ruleId = :ruleId)
              and (:ruleName is null
                   or lower(r.name) like lower(concat('%', :ruleName, '%')))
            order by a.createdTime desc
            """)
    Page<Alert> search(
            @Param("severity") Severity severity,
            @Param("status") AlertStatus status,
            @Param("ruleId") Long ruleId,
            @Param("ruleName") String ruleName,
            Pageable pageable
    );
}
