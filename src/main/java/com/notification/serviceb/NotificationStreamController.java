package com.notification.serviceb;

import com.notification.common.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final NotificationStreamBuilder streamBuilder;

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<NotificationDto> getNotificationStream(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String filter,
            ServerWebExchange exchange) {

        log.info("Received notification stream request - userId: {}, limit: {}, filter: {}", userId, limit, filter);

        if (userId == null || userId <= 0) {
            log.warn("Invalid userId provided: {}", userId);
            return Flux.error(new IllegalArgumentException("userId must be positive"));
        }

        if (limit != null && limit <= 0) {
            log.warn("Invalid limit provided: {}", limit);
            return Flux.error(new IllegalArgumentException("limit must be positive"));
        }

        log.info("Starting notification stream for userId: {}", userId);

        return streamBuilder.buildNotificationStream(userId, limit, filter)
                .doFinally(signal -> {
                    log.info("Notification stream ended for userId: {} with signal: {}", userId, signal);
                });
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service B is healthy");
    }
}
