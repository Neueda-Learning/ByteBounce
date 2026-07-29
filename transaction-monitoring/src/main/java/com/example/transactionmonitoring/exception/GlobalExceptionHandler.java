package com.example.transactionmonitoring.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts application and validation exceptions into consistent HTTP errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setTitle("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidAlertStatusTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAlertStatusTransition(
            InvalidAlertStatusTransitionException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("Invalid alert status transition");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(InvalidRuleConfigurationException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRuleConfiguration(
            InvalidRuleConfigurationException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
        problem.setTitle("Invalid rule configuration");
        return ResponseEntity.badRequest().body(problem);
    }

        @ExceptionHandler(UnsupportedCurrencyException.class)
        public ResponseEntity<ProblemDetail> handleUnsupportedCurrency(
                        UnsupportedCurrencyException exception
        ) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                exception.getMessage()
                );
                problem.setTitle("Unsupported currency");
                return ResponseEntity.badRequest().body(problem);
        }

        @ExceptionHandler(AccountSuspendedException.class)
        public ResponseEntity<ProblemDetail> handleAccountSuspended(
                        AccountSuspendedException exception
        ) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.FORBIDDEN,
                                exception.getMessage()
                );
                problem.setTitle("Account suspended");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
        }

        @ExceptionHandler(TransactionLimitExceededException.class)
        public ResponseEntity<ProblemDetail> handleTransactionLimitExceeded(
                        TransactionLimitExceededException exception
        ) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                exception.getMessage()
                );
                problem.setTitle("Transaction limit exceeded");
                return ResponseEntity.badRequest().body(problem);
        }
}
