package com.chatwoot.api.account.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UserProfileResponse(
        String accessToken,
        Integer accountId,
        String availableName,
        String avatarUrl,
        boolean confirmed,
        String displayName,
        String messageSignature,
        String email,
        Integer id,
        Long inviterId,
        String name,
        String provider,
        String pubsubToken,
        String role,
        Map<String, Object> uiSettings,
        String uid,
        String type,
        List<ProfileAccountResponse> accounts
) {
    public record ProfileAccountResponse(
            Integer id,
            String name,
            String status,
            Instant activeAt,
            String role,
            List<String> permissions,
            String availability,
            String availabilityStatus,
            boolean autoOffline,
            boolean apiAndWebhooks
    ) {
    }
}
