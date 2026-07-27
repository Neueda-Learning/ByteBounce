package com.example.transactionmonitoring.controller;

import com.example.transactionmonitoring.dto.AlertResponse;
import com.example.transactionmonitoring.dto.AlertDetailsResponse;
import com.example.transactionmonitoring.dto.AlertStatusUpdateRequest;
import com.example.transactionmonitoring.dto.AlertTimelineResponse;
import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.entity.AlertStatus;
import com.example.transactionmonitoring.entity.Severity;
import com.example.transactionmonitoring.service.AlertDetailsService;
import com.example.transactionmonitoring.service.AlertService;
import com.example.transactionmonitoring.service.AlertTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for querying alerts and managing their lifecycle.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(
        name = "Alerts",
        description = "Retrieve generated alerts and manage their lifecycle."
)
public class AlertController {

    private final AlertService alertService;
    private final AlertDetailsService alertDetailsService;
    private final AlertTimelineService alertTimelineService;

    @GetMapping
    @Operation(
            summary = "Retrieve all alerts",
            description = "Retrieve generated alerts as a paginated result."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Alerts retrieved successfully"
    )
    public ResponseEntity<PageResponse<AlertResponse>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(alertService.getAllAlerts(page, size));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search alerts",
            description = "Filter alerts by severity, status, rule identifier or rule name with pagination."
    )
    public ResponseEntity<PageResponse<AlertResponse>> searchAlerts(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String ruleName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                alertService.searchAlerts(
                        severity,
                        status,
                        ruleId,
                        ruleName,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Retrieve an alert",
            description = "Return one generated alert by its identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert retrieved"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertResponse> getAlertById(
            @Parameter(description = "Alert identifier", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @GetMapping("/{id}/details")
    @Operation(
            summary = "Retrieve alert details",
            description = "Return an alert together with its triggering transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert details retrieved"),
            @ApiResponse(responseCode = "404", description = "Alert or transaction not found")
    })
    public ResponseEntity<AlertDetailsResponse> getAlertDetails(
            @Parameter(description = "Alert identifier", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(alertDetailsService.getAlertDetails(id));
    }

    @GetMapping("/{id}/timeline")
    @Operation(
            summary = "Retrieve alert timeline",
            description = "Return the alert lifecycle in chronological order."
    )
    public ResponseEntity<List<AlertTimelineResponse>> getAlertTimeline(
            @Parameter(description = "Alert identifier", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(alertTimelineService.getTimeline(id));
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Update alert status",
            description = "Update alert status and create alert history record."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Alert not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<AlertResponse> updateAlertStatus(
            @Parameter(description = "Alert identifier", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Target lifecycle status",
                    required = true
            )
            @Valid @RequestBody AlertStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                alertService.updateAlertStatus(id, request.status())
        );
    }
}
