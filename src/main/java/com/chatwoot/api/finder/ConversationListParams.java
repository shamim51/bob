package com.chatwoot.api.finder;

public record ConversationListParams(
        String status,
        String assigneeType,
        Integer inboxId,
        Integer page,
        String sortBy,
        String q
) {
}
