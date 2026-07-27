package com.example.transactionmonitoring.repository;

import com.example.transactionmonitoring.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access operations for transactions.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("select coalesce(sum(t.amount), 0) from Transaction t")
    BigDecimal sumTotalAmount();

    List<Transaction> findByAccountId(String accountId);

    List<Transaction> findByPayeeId(String payeeId);

    List<Transaction> findByTransactionTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<Transaction> findByAccountIdAndTransactionTimeBetween(
            String accountId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    long countByAccountIdAndTransactionTimeBetween(
            String accountId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    boolean existsByAccountIdAndPayeeIdAndIdNot(
            String accountId,
            String payeeId,
            Long excludedTransactionId
    );

    @Query("""
            select t
            from Transaction t
            where (:accountId is null
                   or lower(t.accountId) like lower(concat('%', :accountId, '%')))
              and (:payeeId is null
                   or lower(t.payeeId) like lower(concat('%', :payeeId, '%')))
              and (:type is null or t.type = :type)
              and (:minAmount is null or t.amount >= :minAmount)
              and (:maxAmount is null or t.amount <= :maxAmount)
            order by t.transactionTime desc
            """)
    Page<Transaction> search(
            @Param("accountId") String accountId,
            @Param("payeeId") String payeeId,
            @Param("type") String type,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable
    );
}
