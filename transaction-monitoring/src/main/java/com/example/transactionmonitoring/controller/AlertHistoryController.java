package com.example.transactionmonitoring.controller;

import com.example.transactionmonitoring.dto.AlertHistoryResponse;
import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.service.AlertHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for alert lifecycle audit records.
 */
@RestController
@RequestMapping("/api/alert-history")
@RequiredArgsConstructor
@Tag(
        name = "Alert History",
        description = "Retrieve alert lifecycle status changes."
)
public class AlertHistoryController {

    private final AlertHistoryService alertHistoryService;

    @GetMapping
    @Operation(
            summary = "Retrieve alert history",
            description = "Return alert status transitions in reverse chronological pages."
    )
    public ResponseEntity<PageResponse<AlertHistoryResponse>> getAllHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                alertHistoryService.getAllHistory(page, size)
        );
    }
}
