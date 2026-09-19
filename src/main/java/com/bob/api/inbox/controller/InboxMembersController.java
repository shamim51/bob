package com.bob.api.inbox.controller;

import com.bob.api.account.dto.AgentResponse;
import com.bob.api.account.mapper.AgentMapper;
import com.bob.api.account.model.AccountUser;
import com.bob.api.account.model.User;
import com.bob.api.account.repository.AccountUserRepository;
import com.bob.api.inbox.dto.InboxMembersUpdateRequest;
import com.bob.api.inbox.model.Inbox;
import com.bob.api.inbox.model.InboxMember;
import com.bob.api.inbox.repository.InboxMemberRepository;
import com.bob.api.inbox.repository.InboxRepository;
import com.bob.api.security.CurrentUserService;
import com.bob.api.shared.dto.PayloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/inbox_members")
public class InboxMembersController {

    private final CurrentUserService currentUserService;
    private final InboxRepository inboxes;
    private final InboxMemberRepository members;
    private final AccountUserRepository accountUsers;
    private final AgentMapper agentMapper;

    public InboxMembersController(
            CurrentUserService currentUserService,
            InboxRepository inboxes,
            InboxMemberRepository members,
            AccountUserRepository accountUsers,
            AgentMapper agentMapper
    ) {
        this.currentUserService = currentUserService;
        this.inboxes = inboxes;
        this.members = members;
        this.accountUsers = accountUsers;
        this.agentMapper = agentMapper;
    }

    @PatchMapping
    @Transactional
    public PayloadResponse<List<AgentResponse>> update(
            @PathVariable Integer accountId,
            @RequestBody InboxMembersUpdateRequest request
    ) {
        currentUserService.requireMembership(accountId);
        if (request == null || request.inboxId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "inbox_id is required");
        }
        Inbox inbox = inboxes.findByIdAndAccountId(request.inboxId(), accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Set<Integer> requested = request.userIds() == null ? Set.of() : new HashSet<>(request.userIds());
        Map<Integer, AccountUser> memberships = accountUsers.findByAccount_Id(accountId).stream()
                .collect(Collectors.toMap(membership -> membership.getUser().getId(), membership -> membership));
        for (Integer userId : requested) {
            if (!memberships.containsKey(userId)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "User is not a member of this account");
            }
        }
        List<InboxMember> current = members.findByInboxId(inbox.getId());
        Set<Integer> currentIds = current.stream().map(InboxMember::getUserId).collect(Collectors.toSet());
        List<Integer> toRemove = currentIds.stream().filter(id -> !requested.contains(id)).toList();
        if (!toRemove.isEmpty()) {
            members.deleteByInboxIdAndUserIdIn(inbox.getId(), toRemove);
        }
        for (Integer userId : requested) {
            if (!currentIds.contains(userId)) {
                InboxMember member = new InboxMember();
                member.setInboxId(inbox.getId());
                member.setUserId(userId);
                members.save(member);
            }
        }
        List<AgentResponse> agents = new ArrayList<>();
        for (Integer userId : requested) {
            AccountUser membership = memberships.get(userId);
            User user = membership.getUser();
            user.setCurrentAccountUser(membership);
            agents.add(agentMapper.agent(user, accountId));
        }
        return new PayloadResponse<>(agents);
    }
}
