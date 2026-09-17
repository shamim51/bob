package com.chatwoot.api.inbox.dto;

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
        String businessName
) {
}
