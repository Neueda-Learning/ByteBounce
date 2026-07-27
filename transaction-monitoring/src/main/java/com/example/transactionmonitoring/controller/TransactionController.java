package com.example.transactionmonitoring.controller;

import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.dto.TransactionRequest;
import com.example.transactionmonitoring.dto.TransactionResponse;
import com.example.transactionmonitoring.service.TransactionService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

/**
 * REST endpoints for recording and querying transactions.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(
        name = "Transactions",
        description = "Record transactions and retrieve transaction information."
)
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(
            summary = "Create a transaction",
            description = "Create a new transaction and trigger risk rules."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction request")
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transaction details to record",
                    required = true
            )
            @Valid @RequestBody TransactionRequest request
    ) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity
                .created(URI.create("/api/transactions/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(
            summary = "Retrieve all transactions",
            description = "Return recorded transactions as a paginated result."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully"
    )
    public ResponseEntity<PageResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                transactionService.getAllTransactions(page, size)
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search transactions",
            description = "Filter transactions by account, payee, type and amount range with pagination."
    )
    public ResponseEntity<PageResponse<TransactionResponse>> searchTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String payeeId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(transactionService.searchTransactions(
                accountId,
                payeeId,
                type,
                minAmount,
                maxAmount,
                page,
                size
        ));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Retrieve a transaction",
            description = "Return one transaction by its identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction retrieved"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> getTransactionById(
            @Parameter(description = "Transaction identifier", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}
