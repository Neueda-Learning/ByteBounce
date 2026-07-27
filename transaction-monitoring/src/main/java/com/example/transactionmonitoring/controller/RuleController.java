package com.example.transactionmonitoring.controller;

import com.example.transactionmonitoring.dto.RuleRequest;
import com.example.transactionmonitoring.dto.RuleResponse;
import com.example.transactionmonitoring.dto.RuleStatusUpdateRequest;
import com.example.transactionmonitoring.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for managing monitoring rule configuration.
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Tag(
        name = "Rules",
        description = "Create, retrieve, update, enable, and disable rules."
)
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    @Operation(
            summary = "Retrieve all rules",
            description = "Return all configured monitoring rules."
    )
    public ResponseEntity<List<RuleResponse>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Retrieve a rule",
            description = "Return one monitoring rule by its identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rule retrieved"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<RuleResponse> getRuleById(
            @Parameter(description = "Rule identifier", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ruleService.getRuleById(id));
    }

    @PostMapping
    @Operation(
            summary = "Create a rule",
            description = "Create a new configurable monitoring rule."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rule created"),
            @ApiResponse(responseCode = "400", description = "Invalid rule")
    })
    public ResponseEntity<RuleResponse> createRule(
            @Valid @RequestBody RuleRequest request
    ) {
        RuleResponse response = ruleService.createRule(request);
        return ResponseEntity
                .created(URI.create("/api/rules/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a rule",
            description = "Replace the editable configuration of an existing rule."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rule updated"),
            @ApiResponse(responseCode = "400", description = "Invalid rule"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<RuleResponse> updateRule(
            @Parameter(description = "Rule identifier", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody RuleRequest request
    ) {
        return ResponseEntity.ok(ruleService.updateRule(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Enable or disable a rule",
            description = "Change whether a monitoring rule participates in evaluation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rule status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status request"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<RuleResponse> updateRuleStatus(
            @Parameter(description = "Rule identifier", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody RuleStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ruleService.updateRuleStatus(id, request.enabled())
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Disable a rule",
            description = "Soft-delete a monitoring rule by disabling it while preserving alert references."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rule disabled"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "Rule identifier", example = "1")
            @PathVariable Long id
    ) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
