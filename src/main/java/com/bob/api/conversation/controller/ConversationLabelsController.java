package com.bob.api.conversation.controller;

import com.bob.api.conversation.model.Conversation;
import com.bob.api.conversation.repository.ConversationRepository;
import com.bob.api.security.CurrentUserService;
import com.bob.api.shared.dto.PayloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/conversations/{displayId}/labels")
public class ConversationLabelsController {

    private final CurrentUserService currentUserService;
    private final ConversationRepository conversations;

    public ConversationLabelsController(
            CurrentUserService currentUserService,
            ConversationRepository conversations
    ) {
        this.currentUserService = currentUserService;
        this.conversations = conversations;
    }

    @GetMapping
    public PayloadResponse<List<String>> index(
            @PathVariable Integer accountId,
            @PathVariable Integer displayId
    ) {
        currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new PayloadResponse<>(conversation.labelList());
    }
}
