package com.chatwoot.api.integration.facebook.dto;

public record ReauthorizePageRequest(String omniauthToken, Integer inboxId) {
}
