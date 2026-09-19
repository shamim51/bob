package com.bob.api.messaging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record MessageResponse(
        Integer id,
        String content,
        Integer inboxId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String echoId,
        Integer conversationId,
        Integer messageType,
        String contentType,
        String status,
        Map<String, Object> contentAttributes,
        long createdAt,
        @JsonProperty("private") boolean privateMessage,
        String sourceId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Object sender
) {
}
