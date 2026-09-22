package com.bob.api.conversation.dto;

import java.util.List;

public record ConversationAttachmentsResponse(
        AttachmentMeta meta,
        List<Object> payload
) {
    public record AttachmentMeta(long totalCount) {
    }

    public static ConversationAttachmentsResponse empty() {
        return new ConversationAttachmentsResponse(new AttachmentMeta(0), List.of());
    }
}
