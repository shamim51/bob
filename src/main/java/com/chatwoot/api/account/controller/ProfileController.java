package com.chatwoot.api.account.controller;

import com.chatwoot.api.account.dto.ProfileResponse;
import com.chatwoot.api.account.dto.ProfileUpdateRequest;
import com.chatwoot.api.account.dto.UserProfileResponse;
import com.chatwoot.api.account.mapper.ProfileMapper;
import com.chatwoot.api.account.model.AccountUser;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.account.repository.AccountUserRepository;
import com.chatwoot.api.account.repository.UserRepository;
import com.chatwoot.api.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final ProfileMapper mapper;
    private final UserRepository users;
    private final AccountUserRepository accountUsers;

    public ProfileController(
            CurrentUserService currentUserService,
            ProfileMapper mapper,
            UserRepository users,
            AccountUserRepository accountUsers
    ) {
        this.currentUserService = currentUserService;
        this.mapper = mapper;
        this.users = users;
        this.accountUsers = accountUsers;
    }

    @GetMapping
    public ProfileResponse show() {
        User user = currentUserService.requireUser();
        List<AccountUser> memberships = currentUserService.memberships(user);
        return new ProfileResponse(new ProfileResponse.ProfilePayload(true, mapper.userProfile(user, memberships)));
    }

    @PutMapping
    public UserProfileResponse update(@RequestBody ProfileUpdateRequest request) {
        User user = currentUserService.requireUser();
        ProfileUpdateRequest.ProfileFields profile = request == null ? null : request.profile();
        if (profile != null) {
            if (profile.name() != null) {
                user.setName(profile.name());
            }
            if (profile.displayName() != null) {
                user.setDisplayName(profile.displayName());
            }
            if (profile.messageSignature() != null) {
                user.setMessageSignature(profile.messageSignature());
            }
            if (profile.uiSettings() != null) {
                user.setUiSettings(profile.uiSettings());
            }
            users.save(user);
        }
        List<AccountUser> memberships = currentUserService.memberships(user);
        return mapper.userProfile(user, memberships);
    }

    @PutMapping("/set_active_account")
    public ResponseEntity<Void> setActiveAccount(@RequestBody ProfileUpdateRequest request) {
        User user = currentUserService.requireUser();
        Integer accountId = request == null || request.profile() == null ? null : request.profile().accountId();
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "account_id is required");
        }
        AccountUser membership = accountUsers.findByAccount_IdAndUser_Id(accountId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not a member of this account"));
        membership.setActiveAt(Instant.now());
        accountUsers.save(membership);
        return ResponseEntity.ok().build();
    }
}
