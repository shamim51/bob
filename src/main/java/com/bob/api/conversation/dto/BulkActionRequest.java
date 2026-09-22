package com.bob.api.conversation.dto;

import java.util.List;

public record BulkActionRequest(
        String type,
        List<Integer> ids,
        BulkActionFields fields,
        Object snoozedUntil,
        BulkActionLabels labels
) {
}
