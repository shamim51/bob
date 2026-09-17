package com.chatwoot.api.account.dto;

public record ProfileResponse(ProfilePayload payload) {
    public record ProfilePayload(boolean success, UserProfileResponse data) {
    }
}
