package com.chatwoot.api.conversation.controller;

import com.chatwoot.api.account.dto.AgentResponse;
import com.chatwoot.api.account.mapper.AgentMapper;
import com.chatwoot.api.account.model.AccountUser;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.account.repository.UserRepository;
import com.chatwoot.api.conversation.dto.ConversationCountMeta;
import com.chatwoot.api.conversation.dto.ConversationListResponse;
import com.chatwoot.api.conversation.dto.ConversationMetaEnvelope;
import com.chatwoot.api.conversation.dto.ConversationResponse;
import com.chatwoot.api.conversation.dto.ToggleStatusResponse;
import com.chatwoot.api.conversation.finder.ConversationFinder;
import com.chatwoot.api.conversation.finder.ConversationFinderResult;
import com.chatwoot.api.conversation.finder.ConversationListParams;
import com.chatwoot.api.conversation.mapper.ConversationMapper;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.conversation.repository.ConversationRepository;
import com.chatwoot.api.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/conversations")
public class ConversationsController {

    private static final Logger log = LoggerFactory.getLogger(ConversationsController.class);

    private final CurrentUserService currentUserService;
    private final ConversationFinder conversationFinder;
    private final ConversationRepository conversations;
    private final UserRepository users;
    private final ConversationMapper mapper;
    private final AgentMapper agents;

    public ConversationsController(
            CurrentUserService currentUserService,
            ConversationFinder conversationFinder,
            ConversationRepository conversations,
            UserRepository users,
            ConversationMapper mapper,
            AgentMapper agents
    ) {
        this.currentUserService = currentUserService;
        this.conversationFinder = conversationFinder;
        this.conversations = conversations;
        this.users = users;
        this.mapper = mapper;
        this.agents = agents;
    }

    @GetMapping
    public ConversationListResponse index(
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
        return new ConversationListResponse(new ConversationListResponse.ConversationListData(
                counts(result),
                result.conversations().stream().map(mapper::conversation).toList()
        ));
    }

    @GetMapping("/meta")
    public ConversationMetaEnvelope meta(
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
        return new ConversationMetaEnvelope(counts(result));
    }

    @GetMapping("/{displayId}")
    public ConversationResponse show(@PathVariable Integer accountId, @PathVariable Integer displayId) {
        currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapper.conversation(conversation);
    }

    @PostMapping("/{displayId}/update_last_seen")
    public ConversationResponse updateLastSeen(@PathVariable Integer accountId, @PathVariable Integer displayId) {
        currentUserService.requireMembership(accountId);
        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        conversation.setAgentLastSeenAt(Instant.now());
        conversations.save(conversation);
        return mapper.conversation(conversation);
    }

    @PostMapping("/{displayId}/toggle_status")
    public ToggleStatusResponse toggleStatus(
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
        log.info("CONVERSATION action=toggle_status result=SAVED conversationDisplayId={} status={}",
                conversation.getDisplayId(), conversation.statusName());
        return new ToggleStatusResponse(
                Map.of(),
                new ToggleStatusResponse.ToggleStatusPayload(
                        true,
                        conversation.getDisplayId(),
                        conversation.statusName(),
                        conversation.getSnoozedUntil()
                )
        );
    }

    @PostMapping("/{displayId}/assignments")
    public AgentResponse assign(
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
                log.info("CONVERSATION action=assign result=SAVED conversationDisplayId={} assigneeId=null",
                        conversation.getDisplayId());
                return null;
            }
            Integer assigneeId = ((Number) raw).intValue();
            User assignee = users.findById(assigneeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            conversation.setAssignee(assignee);
            conversations.save(conversation);
            log.info("CONVERSATION action=assign result=SAVED conversationDisplayId={} assigneeId={}",
                    conversation.getDisplayId(), assigneeId);
            assignee.setCurrentAccountUser(membership);
            return agents.agent(assignee, accountId);
        }
        return null;
    }

    private ConversationCountMeta counts(ConversationFinderResult result) {
        return new ConversationCountMeta(
                result.mineCount(),
                result.assignedCount(),
                result.unassignedCount(),
                result.allCount()
        );
    }
}
