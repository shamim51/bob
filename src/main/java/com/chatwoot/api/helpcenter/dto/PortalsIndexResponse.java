package com.chatwoot.api.helpcenter.dto;

import java.util.List;

public record PortalsIndexResponse(List<Object> payload, PortalsMeta meta) {
    public record PortalsMeta(int currentPage, int portalsCount) {
    }
}
