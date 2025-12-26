package com.notification.servicea;

import com.notification.common.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class NotificationClientController {

    private final WebClient webClient;

    @Value("${app.service-a.client-timeout:20000}")
    private long clientTimeout;

    @GetMapping(value = "/notifications", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<NotificationDto> getNotifications(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String filter) {

        log.info("Client received request - userId: {}, limit: {}, filter: {}", userId, limit, filter);

        if (userId == null || userId <= 0) {
            log.warn("Invalid userId in client request: {}", userId);
            return Flux.error(new IllegalArgumentException("userId must be positive"));
        }

        if (limit != null && limit <= 0) {
            log.warn("Invalid limit in client request: {}", limit);
            return Flux.error(new IllegalArgumentException("limit must be positive"));
        }

        return requestNotificationStream(userId, limit, filter);
    }

    private Flux<NotificationDto> requestNotificationStream(Long userId, Integer limit, String filter) {
        log.info("Sending WebClient request to Service B for userId: {}", userId);

        String url = buildServiceBUrl(userId, limit, filter);
        log.debug("Target URL: {}", url);

        return webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_NDJSON)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Service B returned error: {}", body);
                                    return Mono.error(new RuntimeException("Service B error: " + body));
                                })
                )
                .bodyToFlux(NotificationDto.class)
                .timeout(Duration.ofMillis(clientTimeout))
                .doOnNext(notification -> {
                    log.debug("Received notification from Service B - id: {}, type: {}, title: {}",
                            notification.getId(), notification.getType(), notification.getTitle());
                })
                .doOnError(error -> {
                    if (error instanceof IllegalStateException && error.getMessage().contains("timeout")) {
                        log.error("Timeout occurred while streaming notifications ({}ms) for userId: {}",
                                clientTimeout, userId);
                    } else {
                        log.error("Error streaming notifications for userId: {}", userId, error);
                    }
                })
                .doOnCancel(() -> {
                    log.info("Notification stream subscription cancelled for userId: {}", userId);
                })
                .doOnComplete(() -> {
                    log.info("Notification stream completed for userId: {}", userId);
                });
    }

    private String buildServiceBUrl(Long userId, Integer limit, String filter) {
        // ОПТИМИЗАЦИЯ: Избегаем String конкатенации перед StringBuilder
        StringBuilder url = new StringBuilder("http://localhost:8080/api/notifications/stream?userId=")
                .append(userId);

        if (limit != null) {
            url.append("&limit=").append(limit);
        }

        if (filter != null && !filter.isEmpty()) {
            url.append("&filter=").append(filter);
        }

        return url.toString();
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Service A is healthy");
    }
}
