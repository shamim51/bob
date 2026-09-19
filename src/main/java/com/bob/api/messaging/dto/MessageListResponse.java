package com.bob.api.messaging.dto;

import com.bob.api.account.dto.AgentResponse;
import com.bob.api.contact.dto.ContactResponse;

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
