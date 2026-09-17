package com.chatwoot.api.contact.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record ContactResponse(
        Map<String, Object> additionalAttributes,
        String availabilityStatus,
        String email,
        Integer id,
        String name,
        String phoneNumber,
        boolean blocked,
        String identifier,
        String thumbnail,
        Map<String, Object> customAttributes,
        String type,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long lastActivityAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long createdAt
) {
}
