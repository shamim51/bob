package com.chatwoot.api.web.api.v1;

import com.chatwoot.api.domain.AccountUser;
import com.chatwoot.api.domain.Inbox;
import com.chatwoot.api.domain.User;
import com.chatwoot.api.repo.AccountUserRepository;
import com.chatwoot.api.repo.InboxRepository;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.web.dto.ChatwootJson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class SupportingReadController {

    private final CurrentUserService currentUserService;
    private final InboxRepository inboxes;
    private final AccountUserRepository accountUsers;
    private final ChatwootJson json;

    public SupportingReadController(
            CurrentUserService currentUserService,
            InboxRepository inboxes,
            AccountUserRepository accountUsers,
            ChatwootJson json
    ) {
        this.currentUserService = currentUserService;
        this.inboxes = inboxes;
        this.accountUsers = accountUsers;
        this.json = json;
    }

    @GetMapping("/inboxes")
    public Map<String, Object> inboxes(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        List<Map<String, Object>> payload = inboxes.findByAccountId(accountId).stream()
                .map(json::inbox)
                .toList();
        return Map.of("payload", payload);
    }

    @GetMapping("/agents")
    public List<Map<String, Object>> agents(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return accountUsers.findByAccount_Id(accountId).stream()
                .map(membership -> {
                    User user = membership.getUser();
                    user.setCurrentAccountUser(membership);
                    return json.agent(user, accountId);
                })
                .toList();
    }

    @GetMapping("/labels")
    public Map<String, Object> labels(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return Map.of("payload", List.of());
    }

    @GetMapping("/teams")
    public List<Object> teams(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return List.of();
    }

    @GetMapping("/custom_attribute_definitions")
    public List<Object> customAttributes(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return List.of();
    }

    @GetMapping("/custom_filters")
    public List<Object> customFilters(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return List.of();
    }

    @GetMapping("/notifications/unread_count")
    public int unreadCount(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return 0;
    }

    @GetMapping("/notifications")
    public Map<String, Object> notifications(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return Map.of("data", Map.of(
                "payload", List.of(),
                "meta", Map.of("count", 0, "current_page", 1, "unread_count", 0)
        ));
    }
}
