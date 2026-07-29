package com.example.transactionmonitoring.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks an account flagged as risky because one of its transactions
 * triggered a HIGH or MEDIUM severity alert. High-risk accounts have their
 * transactions suspended entirely; medium-risk accounts are limited to a
 * reduced transaction amount until the triggering alert is dismissed.
 */
@Entity
@Table(name = "risk_accounts")
@Getter
@Setter
@NoArgsConstructor
public class RiskAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private Severity riskLevel;

    /**
     * The alert that caused this account to be flagged. Used to release the
     * risk flag once the alert is dismissed.
     */
    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    /**
     * Maximum transaction amount (in base currency) still allowed for
     * medium-risk accounts. Null for high-risk accounts (fully suspended).
     * For medium-risk accounts this is always half of the triggering rule's
     * threshold, falling back to a default base limit when the rule has no
     * numeric threshold (e.g. VELOCITY, NEW_PAYEE).
     */
    @Column(name = "transaction_limit", precision = 19, scale = 4)
    private BigDecimal transactionLimit;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @PrePersist
    void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
