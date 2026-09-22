package com.bob.api.contact.controller;

import com.bob.api.account.model.AccountUser;
import com.bob.api.contact.repository.ContactRepository;
import com.bob.api.conversation.dto.ConversationResponse;
import com.bob.api.conversation.finder.ConversationFinder;
import com.bob.api.conversation.mapper.ConversationMapper;
import com.bob.api.security.CurrentUserService;
import com.bob.api.shared.dto.PayloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/contacts/{contactId}/conversations")
public class ContactConversationsController {

    private final CurrentUserService currentUserService;
    private final ContactRepository contacts;
    private final ConversationFinder conversationFinder;
    private final ConversationMapper mapper;

    public ContactConversationsController(
            CurrentUserService currentUserService,
            ContactRepository contacts,
            ConversationFinder conversationFinder,
            ConversationMapper mapper
    ) {
        this.currentUserService = currentUserService;
        this.contacts = contacts;
        this.conversationFinder = conversationFinder;
        this.mapper = mapper;
    }

    @GetMapping
    public PayloadResponse<List<ConversationResponse>> index(
            @PathVariable Integer accountId,
            @PathVariable Integer contactId,
            @RequestParam(name = "conversation_id", required = false) Integer conversationId
    ) {
        AccountUser membership = currentUserService.requireMembership(accountId);
        contacts.findByIdAndAccountId(contactId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<ConversationResponse> payload = conversationFinder
                .forContact(membership.getUser(), membership, contactId, conversationId)
                .stream()
                .map(mapper::conversation)
                .toList();
        return new PayloadResponse<>(payload);
    }
}
