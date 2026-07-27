package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertHistoryResponse;
import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.entity.AlertHistory;
import com.example.transactionmonitoring.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides read-only access to alert lifecycle audit records.
 */
@Service
@RequiredArgsConstructor
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<AlertHistoryResponse> getAllHistory(
            int page,
            int size
    ) {
        Page<AlertHistoryResponse> historyPage =
                alertHistoryRepository.findAllByOrderByChangedTimeDesc(
                                PageRequest.of(
                                        Math.max(page, 0),
                                        Math.max(size, 1)
                                )
                        )
                        .map(AlertHistoryService::toResponse);
        return PageResponse.from(historyPage);
    }

    private static AlertHistoryResponse toResponse(AlertHistory history) {
        return new AlertHistoryResponse(
                history.getId(),
                history.getAlertId(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getChangedTime()
        );
    }
}
