package com.chatwoot.api.inbox.controller;

import com.chatwoot.api.inbox.dto.InboxResponse;
import com.chatwoot.api.inbox.mapper.InboxMapper;
import com.chatwoot.api.inbox.repository.InboxRepository;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.shared.dto.PayloadResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class InboxesController {

    private final CurrentUserService currentUserService;
    private final InboxRepository inboxes;
    private final InboxMapper mapper;

    public InboxesController(
            CurrentUserService currentUserService,
            InboxRepository inboxes,
            InboxMapper mapper
    ) {
        this.currentUserService = currentUserService;
        this.inboxes = inboxes;
        this.mapper = mapper;
    }

    @GetMapping("/inboxes")
    public PayloadResponse<List<InboxResponse>> inboxes(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return new PayloadResponse<>(inboxes.findByAccountId(accountId).stream().map(mapper::inbox).toList());
    }
}
