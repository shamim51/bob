package com.bob.api.account.dto;

public record AgentResponse(
        Integer id,
        Integer accountId,
        String availabilityStatus,
        boolean autoOffline,
        boolean confirmed,
        String email,
        String provider,
        String availableName,
        String name,
        String role,
        String thumbnail,
        String type
) {
}
