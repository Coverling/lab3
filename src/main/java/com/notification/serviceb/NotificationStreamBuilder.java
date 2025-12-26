package com.notification.serviceb;

import com.notification.common.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class NotificationStreamBuilder {

    // ОПТИМИЗАЦИЯ 1: Статические thread-safe DateTimeFormatter (вместо создания в каждом вызове)
    private static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SYSTEM_PREFIX = "[SYSTEM] ";
    private static final String USER_PREFIX = "[USER] ";
    private static final String SOCIAL_PREFIX = "[SOCIAL] ";

    public Flux<NotificationDto> buildNotificationStream(Long userId, Integer limit, String filter) {
        log.debug("Building notification stream for userId: {}, limit: {}, filter: {}", userId, limit, filter);

        // ОПТИМИЗАЦИЯ 2: Используем Flux.merge для объединения потоков вместо вложенных циклов
        return Flux.merge(
                Flux.fromIterable(generateSystemNotifications(userId, limit)),
                Flux.fromIterable(generateUserNotifications(userId, limit)),
                Flux.fromIterable(generateSocialNotifications(userId, limit))
        )
        // ОПТИМИЗАЦИЯ 3: Одна фильтрация вместо множественных
        .filter(n -> !n.isRead())
        .filter(n -> filter == null || filter.isEmpty() || n.getType().equalsIgnoreCase(filter))
        // Собираем в список для сортировки (reactive streams не гарантируют порядок)
        .collectList()
        .flatMapMany(notifications -> {
            // ОПТИМИЗАЦИЯ 4: Collections.sort() O(n log n) вместо Bubble Sort O(n²)
            List<NotificationDto> sorted = notifications.stream()
                    .sorted(Comparator.comparing(NotificationDto::getCreatedAt).reversed())
                    .limit(limit != null ? limit : Long.MAX_VALUE)
                    .collect(Collectors.toList());

            log.info("Generated {} notifications for userId: {}", sorted.size(), userId);
            return Flux.fromIterable(sorted);
        })
        // ОПТИМИЗАЦИЯ 5: Batching для более эффективной обработки
        .buffer(100)
        .flatMap(batch ->
            Flux.fromIterable(batch)
                // ОПТИМИЗАЦИЯ 6: flatMap с ограниченной concurrency вместо sequential map
                .flatMap(this::formatNotificationOptimized, 4)
                .subscribeOn(Schedulers.parallel()),
            4  // Обрабатываем до 4 батчей параллельно
        )
        .doOnCancel(() -> log.info("Notification stream subscription cancelled for userId: {}", userId))
        .doOnError(error -> log.error("Error in notification stream for userId: {}", userId, error))
        .doOnComplete(() -> log.info("Notification stream completed for userId: {}", userId));
    }

    private List<NotificationDto> generateSystemNotifications(Long userId, Integer limit) {
        int size = limit != null ? Math.min(limit, 3) : 3;

        // ОПТИМИЗАЦИЯ 7: Stream API для генерации вместо циклов
        return Stream.iterate(0, i -> i + 1)
                .limit(size)
                .map(i -> NotificationDto.builder()
                        .id((long) i)
                        .userId(userId)
                        .type("SYSTEM")
                        // ОПТИМИЗАЦИЯ 8: String.format вместо конкатенации
                        .title(String.format("System Notification %d", i + 1))
                        .message(String.format("System message %d", i + 1))
                        .source("SYSTEM")
                        .read(false)
                        .createdAt(LocalDateTime.now().minusHours(i))
                        .build())
                .collect(Collectors.toList());
    }

    private List<NotificationDto> generateUserNotifications(Long userId, Integer limit) {
        int size = limit != null ? Math.min(limit, 3) : 3;

        return Stream.iterate(0, i -> i + 1)
                .limit(size)
                .map(i -> NotificationDto.builder()
                        .id((long) (100 + i))
                        .userId(userId)
                        .type("USER")
                        .title(String.format("User Notification %d", i + 1))
                        .message(String.format("User message %d", i + 1))
                        .source("USER")
                        .read(false)
                        .createdAt(LocalDateTime.now().minusHours(i + 3))
                        .build())
                .collect(Collectors.toList());
    }

    private List<NotificationDto> generateSocialNotifications(Long userId, Integer limit) {
        int size = limit != null ? Math.min(limit, 3) : 3;

        return Stream.iterate(0, i -> i + 1)
                .limit(size)
                .map(i -> NotificationDto.builder()
                        .id((long) (200 + i))
                        .userId(userId)
                        .type("SOCIAL")
                        .title(String.format("Social Notification %d", i + 1))
                        .message(String.format("Social message %d", i + 1))
                        .source("SOCIAL")
                        .read(false)
                        .createdAt(LocalDateTime.now().minusHours(i + 6))
                        .build())
                .collect(Collectors.toList());
    }

    // ОПТИМИЗАЦИЯ 9: Упрощенное форматирование без тяжеловесных операций
    private Flux<NotificationDto> formatNotificationOptimized(NotificationDto notification) {
        return Flux.defer(() -> {
            try {
                // УДАЛЕНО: synchronized блок (DateTimeFormatter thread-safe)
                // УДАЛЕНО: Множественные преобразования дат
                // УДАЛЕНО: 6x JSON сериализация/десериализация
                // УДАЛЕНО: Создание временного объекта через Builder

                // Простое нормализование даты до полудня (12:00:00)
                LocalDateTime normalized = notification.getCreatedAt()
                        .withHour(12)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);

                notification.setCreatedAt(normalized);

                // ОПТИМИЗАЦИЯ 10: Switch expression вместо множественных if с дублированием
                String prefix = switch (notification.getType()) {
                    case "SYSTEM" -> SYSTEM_PREFIX;
                    case "USER" -> USER_PREFIX;
                    case "SOCIAL" -> SOCIAL_PREFIX;
                    default -> "";
                };

                // Добавляем префикс, только если его еще нет
                if (!notification.getTitle().startsWith(prefix)) {
                    notification.setTitle(prefix + notification.getTitle());
                }

                log.debug("Formatted notification: id={}, type={}, title={}",
                        notification.getId(), notification.getType(), notification.getTitle());

                return Flux.just(notification);
            } catch (Exception e) {
                log.error("Error formatting notification", e);
                return Flux.error(e);
            }
        });
    }
}
