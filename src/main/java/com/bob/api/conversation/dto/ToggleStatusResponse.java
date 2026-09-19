package com.bob.api.conversation.dto;

import java.time.Instant;
import java.util.Map;

public record ToggleStatusResponse(
        Map<String, Object> meta,
        ToggleStatusPayload payload
) {
    public record ToggleStatusPayload(
            boolean success,
            Integer conversationId,
            String currentStatus,
            Instant snoozedUntil
    ) {
    }
}
