package com.example.transactionmonitoring.controller;

import com.example.transactionmonitoring.dto.DashboardStatisticsResponse;
import com.example.transactionmonitoring.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for dashboard aggregate statistics.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(
        name = "Dashboard",
        description = "Aggregate transaction and alert statistics."
)
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/statistics")
    @Operation(
            summary = "Retrieve dashboard statistics",
            description = "Return aggregate transaction and alert totals."
    )
    public ResponseEntity<DashboardStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(dashboardService.getStatistics());
    }
}
