package com.example.transactionmonitoring.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.transactionmonitoring.service.SseNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Exposes a Server-Sent Events stream so the frontend can react to
 * asynchronous rule-engine results without polling or manual refreshes.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(
        name = "Notifications",
        description = "Real-time push notifications consumed by the frontend."
)
public class NotificationController {

    private final SseNotificationService sseNotificationService;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Subscribe to real-time updates",
            description = "Opens a Server-Sent Events stream that emits an event whenever "
                    + "a transaction has finished being evaluated by the rule engine."
    )
    public SseEmitter stream() {
        return sseNotificationService.subscribe();
    }
}
