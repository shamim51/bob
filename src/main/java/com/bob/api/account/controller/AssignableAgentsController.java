package com.bob.api.account.controller;

import com.bob.api.account.dto.AgentResponse;
import com.bob.api.account.mapper.AgentMapper;
import com.bob.api.account.model.AccountUser;
import com.bob.api.account.model.User;
import com.bob.api.account.repository.AccountUserRepository;
import com.bob.api.inbox.model.Inbox;
import com.bob.api.inbox.model.InboxMember;
import com.bob.api.inbox.repository.InboxMemberRepository;
import com.bob.api.inbox.repository.InboxRepository;
import com.bob.api.security.CurrentUserService;
import com.bob.api.shared.dto.PayloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class AssignableAgentsController {

    private final CurrentUserService currentUserService;
    private final InboxRepository inboxes;
    private final InboxMemberRepository inboxMembers;
    private final AccountUserRepository accountUsers;
    private final AgentMapper mapper;

    public AssignableAgentsController(
            CurrentUserService currentUserService,
            InboxRepository inboxes,
            InboxMemberRepository inboxMembers,
            AccountUserRepository accountUsers,
            AgentMapper mapper
    ) {
        this.currentUserService = currentUserService;
        this.inboxes = inboxes;
        this.inboxMembers = inboxMembers;
        this.accountUsers = accountUsers;
        this.mapper = mapper;
    }

    @GetMapping("/assignable_agents")
    public PayloadResponse<List<AgentResponse>> index(
            @PathVariable Integer accountId,
            @RequestParam(name = "inbox_ids[]", required = false) List<Integer> inboxIdsBracket,
            @RequestParam(name = "inbox_ids", required = false) List<Integer> inboxIdsPlain,
            @RequestParam(name = "include_ai_assignees", required = false) String includeAiAssignees
    ) {
        AccountUser membership = currentUserService.requireMembership(accountId);
        List<Integer> inboxIds = inboxIdsBracket != null && !inboxIdsBracket.isEmpty() ? inboxIdsBracket : inboxIdsPlain;
        if (inboxIds == null || inboxIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<Set<Integer>> memberIdSets = new ArrayList<>();
        for (Integer inboxId : inboxIds) {
            Inbox inbox = inboxes.findByIdAndAccountId(inboxId, accountId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            authorizeInboxShow(membership, inbox);
            Set<Integer> memberIds = new HashSet<>();
            for (InboxMember member : inboxMembers.findByInboxId(inbox.getId())) {
                memberIds.add(member.getUserId());
            }
            memberIdSets.add(memberIds);
        }

        Set<Integer> intersection = new HashSet<>(memberIdSets.getFirst());
        for (int i = 1; i < memberIdSets.size(); i++) {
            intersection.retainAll(memberIdSets.get(i));
        }

        Map<Integer, AccountUser> membershipsByUserId = new LinkedHashMap<>();
        for (AccountUser accountUser : accountUsers.findByAccount_Id(accountId)) {
            membershipsByUserId.put(accountUser.getUser().getId(), accountUser);
        }

        Map<Integer, AccountUser> assignable = new LinkedHashMap<>();
        for (Integer userId : intersection) {
            AccountUser agentMembership = membershipsByUserId.get(userId);
            if (agentMembership != null) {
                assignable.put(userId, agentMembership);
            }
        }
        for (AccountUser accountUser : membershipsByUserId.values()) {
            if (accountUser.administrator()) {
                assignable.put(accountUser.getUser().getId(), accountUser);
            }
        }

        boolean includeAi = includeAiAssignees != null && !includeAiAssignees.isBlank();
        String assigneeType = includeAi ? "User" : null;
        List<AgentResponse> payload = assignable.values().stream()
                .map(accountUser -> {
                    User user = accountUser.getUser();
                    user.setCurrentAccountUser(accountUser);
                    return mapper.agent(user, accountId, assigneeType);
                })
                .toList();
        return new PayloadResponse<>(payload);
    }

    private void authorizeInboxShow(AccountUser membership, Inbox inbox) {
        if (membership.administrator()) {
            return;
        }
        Integer userId = membership.getUser().getId();
        boolean assigned = inboxMembers.findByInboxId(inbox.getId()).stream()
                .anyMatch(member -> member.getUserId().equals(userId));
        if (!assigned) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
