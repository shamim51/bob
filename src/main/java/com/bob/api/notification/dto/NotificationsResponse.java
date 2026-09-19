package com.bob.api.notification.dto;

import java.util.List;

public record NotificationsResponse(NotificationsData data) {
    public record NotificationsData(List<Object> payload, NotificationsMeta meta) {
    }

    public record NotificationsMeta(int count, int currentPage, int unreadCount) {
    }
}
