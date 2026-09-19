package com.bob.api.integration.facebook.dto;

public record RegisterFacebookPageRequest(
        String userAccessToken,
        String pageAccessToken,
        String pageId,
        String inboxName
) {
}
