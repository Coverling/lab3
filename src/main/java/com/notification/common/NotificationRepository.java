package com.notification.common;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, Long> {
    Flux<Notification> findByUserId(Long userId);

    Flux<Notification> findByUserIdAndReadFalse(Long userId);

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    Flux<Notification> findByUserIdWithLimit(Long userId, int limit);

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND type = :type AND read = false")
    Flux<Notification> findByUserIdAndType(Long userId, String type);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId")
    Mono<Long> countByUserId(Long userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read = false")
    Mono<Long> countUnreadByUserId(Long userId);
}
