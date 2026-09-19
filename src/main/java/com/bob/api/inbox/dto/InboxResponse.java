package com.bob.api.inbox.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public record InboxResponse(
        Integer id,
        String avatarUrl,
        Integer channelId,
        String name,
        String channelType,
        Boolean greetingEnabled,
        String greetingMessage,
        Boolean workingHoursEnabled,
        Boolean enableEmailCollect,
        Boolean csatSurveyEnabled,
        Map<String, Object> csatConfig,
        Boolean enableAutoAssignment,
        Map<String, Object> autoAssignmentConfig,
        String outOfOfficeMessage,
        List<Object> workingHours,
        String timezone,
        Boolean allowMessagesAfterResolved,
        boolean lockToSingleConversation,
        String senderNameType,
        String businessName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String pageId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String providerName,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean reauthorizationRequired
) {
}
