package com.chatwoot.api.integration.facebook.dto;

public record FacebookRegisterInboxResponse(
        Integer id,
        Integer channelId,
        String name,
        String channelType,
        String avatarUrl,
        String pageId,
        Boolean enableAutoAssignment
) {
}
