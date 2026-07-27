package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.AlertTimelineResponse;
import com.example.transactionmonitoring.entity.Alert;
import com.example.transactionmonitoring.entity.AlertHistory;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.exception.ResourceNotFoundException;
import com.example.transactionmonitoring.repository.AlertHistoryRepository;
import com.example.transactionmonitoring.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the chronological lifecycle timeline for an alert.
 */
@Service
@RequiredArgsConstructor
public class AlertTimelineService {

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    @Transactional(readOnly = true)
    public List<AlertTimelineResponse> getTimeline(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alert not found with id: " + alertId
                ));
        List<AlertHistory> history =
                alertHistoryRepository.findByAlertIdOrderByChangedTimeAsc(alertId);

        AlertStatus initialStatus = history.isEmpty()
                ? alert.getStatus()
                : history.get(0).getOldStatus();
        List<AlertTimelineResponse> timeline = new ArrayList<>();
        timeline.add(new AlertTimelineResponse(initialStatus, alert.getCreatedTime()));
        history.stream()
                .map(item -> new AlertTimelineResponse(
                        item.getNewStatus(),
                        item.getChangedTime()
                ))
                .forEach(timeline::add);
        return timeline;
    }
}
