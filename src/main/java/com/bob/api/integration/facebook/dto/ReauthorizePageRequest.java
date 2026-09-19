package com.bob.api.integration.facebook.dto;

public record ReauthorizePageRequest(String omniauthToken, Integer inboxId) {
}
