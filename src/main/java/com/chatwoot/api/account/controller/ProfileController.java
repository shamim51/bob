package com.chatwoot.api.account.controller;

import com.chatwoot.api.account.dto.ProfileResponse;
import com.chatwoot.api.account.mapper.ProfileMapper;
import com.chatwoot.api.account.model.AccountUser;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final ProfileMapper mapper;

    public ProfileController(CurrentUserService currentUserService, ProfileMapper mapper) {
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    @GetMapping
    public ProfileResponse show() {
        User user = currentUserService.requireUser();
        List<AccountUser> memberships = currentUserService.memberships(user);
        return new ProfileResponse(new ProfileResponse.ProfilePayload(true, mapper.userProfile(user, memberships)));
    }
}
