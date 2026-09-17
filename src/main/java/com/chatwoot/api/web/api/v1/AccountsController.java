package com.chatwoot.api.web.api.v1;

import com.chatwoot.api.domain.Account;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.web.dto.ChatwootJson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class AccountsController {

    private final CurrentUserService currentUserService;
    private final ChatwootJson json;

    public AccountsController(CurrentUserService currentUserService, ChatwootJson json) {
        this.currentUserService = currentUserService;
        this.json = json;
    }

    @GetMapping
    public Map<String, Object> show(@PathVariable Integer accountId) {
        Account account = currentUserService.requireMembership(accountId).getAccount();
        return json.account(account);
    }

    @GetMapping("/cache_keys")
    public Map<String, Object> cacheKeys(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return Map.of("cache_keys", Map.of(
                "label", "0",
                "inbox", "0",
                "team", "0",
                "canned_response", "0"
        ));
    }
}
