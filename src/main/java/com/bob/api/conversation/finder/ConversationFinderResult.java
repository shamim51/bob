package com.bob.api.conversation.finder;

import com.bob.api.conversation.model.Conversation;

import java.util.List;

public record ConversationFinderResult(
        List<Conversation> conversations,
        long mineCount,
        long assignedCount,
        long unassignedCount,
        long allCount
) {
}
