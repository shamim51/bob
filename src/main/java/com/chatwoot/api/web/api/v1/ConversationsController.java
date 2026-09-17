package com.chatwoot.api.web.api.v1;

import com.chatwoot.api.domain.AccountUser;
import com.chatwoot.api.domain.Conversation;
import com.chatwoot.api.domain.User;
import com.chatwoot.api.finder.ConversationFinder;
import com.chatwoot.api.finder.ConversationFinderResult;
import com.chatwoot.api.finder.ConversationListParams;
import com.chatwoot.api.repo.ConversationRepository;
import com.chatwoot.api.repo.UserRepository;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.web.dto.ChatwootJson;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/conversations")
public class ConversationsController {

    private final CurrentUserService currentUserService;
    private final ConversationFinder conversationFinder;
    private final ConversationRepository conversations;
    private final UserRepository users;
    private final ChatwootJson json;

    public ConversationsController(
            CurrentUserService currentUserService,
            ConversationFinder conversationFinder,
            ConversationRepository conversations,
            UserRepository users,
            ChatwootJson json
    ) {
        this.currentUserService = currentUserService;
        this.conversationFinder = conversationFinder;
        this.conversations = conversations;
        this.users = users;
        this.json = json;
    }

    @GetMapping
    public Map<String, Object> index(
            @PathVariable Integer accountId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "assignee_type", required = false) String assigneeType,
            @RequestParam(name = "inbox_id", required = false) Integer inboxId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "q", required = false) String q
    ) {
        AccountUser membership = currentUserService.requireMembership(accountId);
        ConversationFinderResult result = conversationFinder.perform(
                membership.getUser(),
                membership,
                new ConversationListParams(status, assigneeType, inboxId, page, sortBy, q)
        );
        Map<String, Object> meta = counts(result);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("meta", meta);
        data.put("payload", result.conversations().stream().map(json::conversation).toList());
        return Map.of("data", data);
    }

    @GetMapping("/meta")
    public Map<String, Object> meta(
            @PathVariable Integer accountId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "assignee_type", required = false) String assigneeType,
            @RequestParam(name = "inbox_id", required = false) Integer inboxId
    ) {
        AccountUser membership = currentUserService.requireMembership(accountId);
        ConversationFinderResult result = conversationFinder.performMetaOnly(
                membership.getUser(),
                membership,
                new ConversationListParams(status, assigneeType, inboxId, 1, null, null)
        );
        return Map.of("meta", counts(result));
    }

    @GetMapping("/{displayId}")
    public Map<String, Object> show(@PathVariable Integer accountId, @PathVariable Integer displayId) {
        currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return json.conversation(conversation);
    }

    @PostMapping("/{displayId}/update_last_seen")
    public Map<String, Object> updateLastSeen(@PathVariable Integer accountId, @PathVariable Integer displayId) {
        currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        conversation.setAgentLastSeenAt(Instant.now());
        conversations.save(conversation);
        return json.conversation(conversation);
    }

    @PostMapping("/{displayId}/toggle_status")
    public Map<String, Object> toggleStatus(
            @PathVariable Integer accountId,
            @PathVariable Integer displayId,
            @RequestBody Map<String, Object> body
    ) {
        currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String status = String.valueOf(body.getOrDefault("status", "open"));
        conversation.setStatus(Conversation.statusFromName(status));
        if ("snoozed".equals(status) && body.get("snoozed_until") != null) {
            Object raw = body.get("snoozed_until");
            if (raw instanceof Number number) {
                conversation.setSnoozedUntil(Instant.ofEpochSecond(number.longValue()));
            }
        }
        conversations.save(conversation);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("conversation_id", conversation.getDisplayId());
        payload.put("current_status", conversation.statusName());
        payload.put("snoozed_until", conversation.getSnoozedUntil());
        return Map.of("meta", Map.of(), "payload", payload);
    }

    @PostMapping("/{displayId}/assignments")
    public Object assign(
            @PathVariable Integer accountId,
            @PathVariable Integer displayId,
            @RequestBody Map<String, Object> body
    ) {
        AccountUser membership = currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (body.containsKey("assignee_id")) {
            Object raw = body.get("assignee_id");
            if (raw == null) {
                conversation.setAssignee(null);
                conversations.save(conversation);
                return null;
            }
            Integer assigneeId = ((Number) raw).intValue();
            User assignee = users.findById(assigneeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            conversation.setAssignee(assignee);
            conversations.save(conversation);
            assignee.setCurrentAccountUser(membership);
            return json.agent(assignee, accountId);
        }
        return null;
    }

    private Map<String, Object> counts(ConversationFinderResult result) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("mine_count", result.mineCount());
        meta.put("assigned_count", result.assignedCount());
        meta.put("unassigned_count", result.unassignedCount());
        meta.put("all_count", result.allCount());
        return meta;
    }
}
