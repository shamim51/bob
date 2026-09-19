package com.bob.api.conversation.dto;

public record ConversationCountMeta(
        long mineCount,
        long assignedCount,
        long unassignedCount,
        long allCount
) {
}
