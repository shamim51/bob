package com.chatwoot.api.conversation.finder;

import com.chatwoot.api.conversation.model.Conversation;

import java.util.List;

public record ConversationFinderResult(
        List<Conversation> conversations,
        long mineCount,
        long assignedCount,
        long unassignedCount,
        long allCount
) {
}
