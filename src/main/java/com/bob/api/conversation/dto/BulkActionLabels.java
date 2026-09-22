package com.bob.api.conversation.dto;

import java.util.List;

public record BulkActionLabels(
        List<String> add,
        List<String> remove
) {
}
