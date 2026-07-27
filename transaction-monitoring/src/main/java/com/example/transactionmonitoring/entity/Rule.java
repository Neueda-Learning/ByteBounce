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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Configurable monitoring rule used by the rule engine.
 */
@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RuleType type;

    @Column(length = 500)
    private String description;

    /**
     * Amount limit used by amount-threshold and daily-limit rules.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal threshold;

    /**
     * Rolling time window in minutes, primarily used by velocity rules.
     */
    @Column(name = "time_window")
    private Integer timeWindow;

    /**
     * Maximum allowed transaction count inside the configured time window.
     */
    @Column(name = "max_count")
    private Integer maxCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
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
