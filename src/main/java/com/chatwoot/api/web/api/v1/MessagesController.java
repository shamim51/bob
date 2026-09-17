package com.chatwoot.api.web.api.v1;

import com.chatwoot.api.builder.MessageBuilder;
import com.chatwoot.api.domain.Conversation;
import com.chatwoot.api.domain.Message;
import com.chatwoot.api.finder.MessageFinder;
import com.chatwoot.api.repo.ConversationRepository;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.web.dto.ChatwootJson;
import com.chatwoot.api.web.dto.MessageHydrator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
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
    private final ChatwootJson json;

    public MessagesController(
            CurrentUserService currentUserService,
            ConversationRepository conversations,
            MessageFinder messageFinder,
            MessageBuilder messageBuilder,
            MessageHydrator hydrator,
            ChatwootJson json
    ) {
        this.currentUserService = currentUserService;
        this.conversations = conversations;
        this.messageFinder = messageFinder;
        this.messageBuilder = messageBuilder;
        this.hydrator = hydrator;
        this.json = json;
    }

    @GetMapping
    public Map<String, Object> index(
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
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("labels", conversation.labelList());
        meta.put("additional_attributes", conversation.getAdditionalAttributes());
        meta.put("contact", conversation.getContact() == null ? null : json.contact(conversation.getContact(), false));
        meta.put("assignee", conversation.getAssignee() == null ? null : json.agent(conversation.getAssignee(), accountId));
        meta.put("agent_last_seen_at", conversation.getAgentLastSeenAt());
        meta.put("assignee_last_seen_at", conversation.getAssigneeLastSeenAt());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", meta);
        body.put("payload", rows.stream().map(json::message).toList());
        return body;
    }

    @PostMapping
    public Map<String, Object> create(
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
        return json.message(message);
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}
