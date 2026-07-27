package com.example.transactionmonitoring.entity;

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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Immutable audit record of an alert status transition.
 */
@Entity
@Table(name = "alert_history")
@Getter
@Setter
@NoArgsConstructor
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false, length = 32)
    private AlertStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 32)
    private AlertStatus newStatus;

    @Column(name = "changed_time", nullable = false, updatable = false)
    private LocalDateTime changedTime;

    @PrePersist
    void onCreate() {
        if (changedTime == null) {
            changedTime = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
