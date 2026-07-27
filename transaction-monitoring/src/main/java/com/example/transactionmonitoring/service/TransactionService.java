package com.example.transactionmonitoring.service;

import com.example.transactionmonitoring.dto.PageResponse;
import com.example.transactionmonitoring.dto.TransactionRequest;
import com.example.transactionmonitoring.dto.TransactionResponse;
import com.example.transactionmonitoring.entity.Transaction;
import com.example.transactionmonitoring.exception.ResourceNotFoundException;
import com.example.transactionmonitoring.repository.AlertRepository;
import com.example.transactionmonitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service responsible for transaction management.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final RuleEngineService ruleEngineService;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(request.accountId());
        transaction.setPayeeId(request.payeeId());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        transaction.setType(request.type());
        transaction.setTransactionTime(LocalDateTime.now(ZoneOffset.UTC));
        transaction.setDescription(request.description());

        Transaction savedTransaction = transactionRepository.save(transaction);
        ruleEngineService.evaluate(savedTransaction);
        return toResponse(
                savedTransaction,
                Math.toIntExact(alertRepository.countByTransactionId(savedTransaction.getId()))
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAllTransactions(
            int page,
            int size
    ) {
        Page<Transaction> transactions = transactionRepository.findAll(
                pageRequest(page, size)
        );
        return toPageResponse(transactions);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> searchTransactions(
            String accountId,
            String payeeId,
            String type,
            BigDecimal minAmount,
            BigDecimal maxAmount
    ) {
        return searchTransactions(
                accountId,
                payeeId,
                type,
                minAmount,
                maxAmount,
                0,
                Integer.MAX_VALUE
        ).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> searchTransactions(
            String accountId,
            String payeeId,
            String type,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int page,
            int size
    ) {
        Page<Transaction> transactions = transactionRepository.search(
                normalize(accountId),
                normalize(payeeId),
                normalizeType(type),
                minAmount,
                maxAmount,
                pageRequest(page, size)
        );
        return toPageResponse(transactions);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(transaction -> toResponse(
                        transaction,
                        Math.toIntExact(alertRepository.countByTransactionId(id))
                ))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + id
                ));
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return List.of();
        }

        List<Long> transactionIds = transactions.stream()
                .map(Transaction::getId)
                .toList();
        Map<Long, Integer> alertCounts = alertRepository
                .countByTransactionIds(transactionIds)
                .stream()
                .collect(Collectors.toMap(
                        count -> count.getTransactionId(),
                        count -> Math.toIntExact(count.getAlertCount())
                ));

        return transactions.stream()
                .map(transaction -> toResponse(
                        transaction,
                        alertCounts.getOrDefault(transaction.getId(), 0)
                ))
                .toList();
    }

    private PageResponse<TransactionResponse> toPageResponse(
            Page<Transaction> transactions
    ) {
        List<TransactionResponse> content = toResponses(transactions.getContent());
        return new PageResponse<>(
                content,
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
        );
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "transactionTime")
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeType(String type) {
        String normalized = normalize(type);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private static TransactionResponse toResponse(
            Transaction transaction,
            int alertCount
    ) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getPayeeId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getTransactionTime(),
                transaction.getDescription(),
                alertCount > 0,
                alertCount
        );
    }
}
