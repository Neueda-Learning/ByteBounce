package com.example.transactionmonitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * An alert generated when a transaction triggers a monitoring rule.
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 32)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertStatus status;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    /**
     * Initializes lifecycle fields while keeping all stored timestamps in UTC.
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (status == null) {
            status = AlertStatus.OPEN;
        }
        if (createdTime == null) {
            createdTime = now;
        }
        updatedTime = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedTime = LocalDateTime.now(ZoneOffset.UTC);
    }
}
