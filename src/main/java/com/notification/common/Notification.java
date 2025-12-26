package com.notification.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("notifications")
public class Notification {
    @Id
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String message;
    private String source;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
