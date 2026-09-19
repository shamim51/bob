package com.bob.api.notification.controller;

import com.bob.api.notification.dto.NotificationsResponse;
import com.bob.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/notifications")
public class NotificationsController {

    private final CurrentUserService currentUserService;

    public NotificationsController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/unread_count")
    public int unreadCount(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return 0;
    }

    @GetMapping
    public NotificationsResponse notifications(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return new NotificationsResponse(new NotificationsResponse.NotificationsData(
                List.of(),
                new NotificationsResponse.NotificationsMeta(0, 1, 0)
        ));
    }
}
