package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.DashboardStatisticsResponse;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides read-only aggregate data for the monitoring dashboard.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getStatistics() {
        return new DashboardStatisticsResponse(
                transactionRepository.count(),
                transactionRepository.sumTotalAmount(),
                alertRepository.count(),
                alertRepository.countByStatus(AlertStatus.OPEN)
        );
    }
}
