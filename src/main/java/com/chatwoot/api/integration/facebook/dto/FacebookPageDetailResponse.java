package com.chatwoot.api.integration.facebook.dto;

public record FacebookPageDetailResponse(
        String id,
        String name,
        String accessToken,
        boolean exists
) {
}
