package com.chatwoot.api.account.dto;

import java.util.Map;

public record ProfileUpdateRequest(ProfileFields profile) {
    public record ProfileFields(
            Map<String, Object> uiSettings,
            String name,
            String displayName,
            String messageSignature,
            Integer accountId
    ) {
    }
}
