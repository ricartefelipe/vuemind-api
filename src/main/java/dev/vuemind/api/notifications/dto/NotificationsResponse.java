package dev.vuemind.api.notifications.dto;

import java.util.List;

public record NotificationsResponse(List<NotificationDto> items) {
}
