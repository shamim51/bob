package com.bob.api.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
        String type,
        @JsonInclude(JsonInclude.Include.NON_NULL) String assigneeType
) {
}
