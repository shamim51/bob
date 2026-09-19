package com.chatwoot.api.inbox.dto;

import java.util.List;

public record InboxMembersUpdateRequest(Integer inboxId, List<Integer> userIds) {
}
