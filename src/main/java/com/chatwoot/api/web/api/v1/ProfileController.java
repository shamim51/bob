package com.chatwoot.api.web.api.v1;

import com.chatwoot.api.domain.AccountUser;
import com.chatwoot.api.domain.User;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.web.dto.ChatwootJson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final ChatwootJson json;

    public ProfileController(CurrentUserService currentUserService, ChatwootJson json) {
        this.currentUserService = currentUserService;
        this.json = json;
    }

    @GetMapping
    public Map<String, Object> show() {
        User user = currentUserService.requireUser();
        List<AccountUser> memberships = currentUserService.memberships(user);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("data", json.userProfile(user, memberships));
        return Map.of("payload", payload);
    }
}
