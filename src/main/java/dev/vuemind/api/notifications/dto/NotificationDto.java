package dev.vuemind.api.notifications.dto;

import java.time.Instant;

public record NotificationDto(String id, String title, String body, boolean read, Instant createdAt) {
}
