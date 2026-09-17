package com.chatwoot.api.messaging.controller;

import com.chatwoot.api.account.mapper.AgentMapper;
import com.chatwoot.api.contact.mapper.ContactMapper;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.conversation.repository.ConversationRepository;
import com.chatwoot.api.messaging.builder.MessageBuilder;
import com.chatwoot.api.messaging.dto.MessageListResponse;
import com.chatwoot.api.messaging.dto.MessageResponse;
import com.chatwoot.api.messaging.finder.MessageFinder;
import com.chatwoot.api.messaging.mapper.MessageHydrator;
import com.chatwoot.api.messaging.mapper.MessageMapper;
import com.chatwoot.api.messaging.model.Message;
import com.chatwoot.api.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/conversations/{displayId}/messages")
public class MessagesController {

    private final CurrentUserService currentUserService;
    private final ConversationRepository conversations;
    private final MessageFinder messageFinder;
    private final MessageBuilder messageBuilder;
    private final MessageHydrator hydrator;
    private final MessageMapper mapper;
    private final ContactMapper contacts;
    private final AgentMapper agents;

    public MessagesController(
            CurrentUserService currentUserService,
            ConversationRepository conversations,
            MessageFinder messageFinder,
            MessageBuilder messageBuilder,
            MessageHydrator hydrator,
            MessageMapper mapper,
            ContactMapper contacts,
            AgentMapper agents
    ) {
        this.currentUserService = currentUserService;
        this.conversations = conversations;
        this.messageFinder = messageFinder;
        this.messageBuilder = messageBuilder;
        this.hydrator = hydrator;
        this.mapper = mapper;
        this.contacts = contacts;
        this.agents = agents;
    }

    @GetMapping
    public MessageListResponse index(
            @PathVariable Integer accountId,
            @PathVariable Integer displayId,
            @RequestParam(name = "before", required = false) Integer before,
            @RequestParam(name = "after", required = false) Integer after
    ) {
        currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Message> rows = messageFinder.perform(conversation, after, before);
        hydrator.hydrate(rows);
        return new MessageListResponse(
                new MessageListResponse.MessageListMeta(
                        conversation.labelList(),
                        conversation.getAdditionalAttributes(),
                        conversation.getContact() == null ? null : contacts.contact(conversation.getContact(), false),
                        conversation.getAssignee() == null ? null : agents.agent(conversation.getAssignee(), accountId),
                        conversation.getAgentLastSeenAt(),
                        conversation.getAssigneeLastSeenAt()
                ),
                rows.stream().map(mapper::message).toList()
        );
    }

    @PostMapping
    public MessageResponse create(
            @PathVariable Integer accountId,
            @PathVariable Integer displayId,
            @RequestBody Map<String, Object> body
    ) {
        var membership = currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = body.get("content_attributes") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        Message message = messageBuilder.perform(
                membership.getUser(),
                conversation,
                new MessageBuilder.CreateMessageParams(
                        body.get("content") == null ? null : String.valueOf(body.get("content")),
                        truthy(body.get("private")),
                        body.get("echo_id") == null ? null : String.valueOf(body.get("echo_id")),
                        body.get("message_type") == null ? null : String.valueOf(body.get("message_type")),
                        body.get("content_type") == null ? null : String.valueOf(body.get("content_type")),
                        attributes,
                        body.get("source_id") == null ? null : String.valueOf(body.get("source_id"))
                )
        );
        hydrator.hydrate(message);
        return mapper.message(message);
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}
