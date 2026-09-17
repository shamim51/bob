package com.chatwoot.api.finder;

import java.util.List;

public record ConversationFinderResult(
        List<com.chatwoot.api.domain.Conversation> conversations,
        long mineCount,
        long assignedCount,
        long unassignedCount,
        long allCount
) {
}
