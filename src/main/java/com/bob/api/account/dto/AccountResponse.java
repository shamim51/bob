package com.bob.api.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

public record AccountResponse(
        Map<String, Object> settings,
        Instant createdAt,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, Object> customAttributes,
        String domain,
        AccountFeaturesResponse features,
        Integer id,
        String locale,
        String name,
        String supportEmail,
        String status,
        CacheKeysResponse cacheKeys
) {
    public record AccountFeaturesResponse(
            boolean inboxManagement,
            boolean agentManagement,
            boolean teamManagement,
            boolean labels,
            boolean channelFacebook,
            boolean captainIntegration,
            boolean captainTasks,
            boolean conversationUnreadCounts
    ) {
        public static AccountFeaturesResponse defaults() {
            return new AccountFeaturesResponse(true, true, true, true, true, false, false, false);
        }
    }

    public record CacheKeysResponse(
            String label,
            String inbox,
            String team,
            String cannedResponse
    ) {
        public static CacheKeysResponse zeros() {
            return new CacheKeysResponse("0", "0", "0", "0");
        }
    }
}
