package com.chatwoot.api.messaging.dto;

import com.chatwoot.api.account.dto.AgentResponse;
import com.chatwoot.api.contact.dto.ContactResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageListResponse(
        MessageListMeta meta,
        List<MessageResponse> payload
) {
    public record MessageListMeta(
            List<String> labels,
            Map<String, Object> additionalAttributes,
            ContactResponse contact,
            AgentResponse assignee,
            Instant agentLastSeenAt,
            Instant assigneeLastSeenAt
    ) {
    }
}
