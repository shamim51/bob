package com.chatwoot.api.integration.facebook.dto;

public record RegisterFacebookPageRequest(
        String userAccessToken,
        String pageAccessToken,
        String pageId,
        String inboxName
) {
}
