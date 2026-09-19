package com.chatwoot.api.integration.facebook.dto;

import java.util.List;

public record FacebookPagesResponse(Data data) {

    public record Data(List<FacebookPageDetailResponse> pageDetails, String userAccessToken) {
    }
}
