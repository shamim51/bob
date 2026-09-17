package com.chatwoot.api.conversation.dto;

import com.chatwoot.api.account.dto.AgentResponse;
import com.chatwoot.api.contact.dto.ContactResponse;
import com.chatwoot.api.messaging.dto.MessageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ConversationResponse(
        ConversationMetaResponse meta,
        @JsonProperty("id") Integer displayId,
        List<MessageResponse> messages,
        Integer accountId,
        String uuid,
        Map<String, Object> additionalAttributes,
        long agentLastSeenAt,
        long assigneeLastSeenAt,
        boolean canReply,
        ContactInfoRequestResponse contactInfoRequest,
        long contactLastSeenAt,
        Map<String, Object> customAttributes,
        Integer inboxId,
        List<String> labels,
        boolean muted,
        Instant snoozedUntil,
        String status,
        long createdAt,
        double updatedAt,
        long timestamp,
        long firstReplyCreatedAt,
        long unreadCount,
        MessageResponse lastNonActivityMessage,
        long lastActivityAt,
        String priority,
        long waitingSince,
        Long slaPolicyId
) {
    public record ConversationMetaResponse(
            @JsonInclude(JsonInclude.Include.NON_NULL) ContactResponse sender,
            String channel,
            @JsonInclude(JsonInclude.Include.NON_NULL) AgentResponse assignee,
            @JsonInclude(JsonInclude.Include.NON_NULL) String assigneeType,
            boolean hmacVerified
    ) {
    }

    public record ContactInfoRequestResponse(
            boolean available,
            String reason,
            String deliveryMode
    ) {
        public static ContactInfoRequestResponse unavailable() {
            return new ContactInfoRequestResponse(false, null, null);
        }
    }
}
