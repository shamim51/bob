package com.chatwoot.api.conversation.dto;

public record ConversationCountMeta(
        long mineCount,
        long assignedCount,
        long unassignedCount,
        long allCount
) {
}
