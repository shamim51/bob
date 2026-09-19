package com.bob.api.conversation.dto;

import java.util.List;

public record ConversationListResponse(ConversationListData data) {
    public record ConversationListData(
            ConversationCountMeta meta,
            List<ConversationResponse> payload
    ) {
    }
}
