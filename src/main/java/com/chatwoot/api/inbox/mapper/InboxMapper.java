package com.chatwoot.api.inbox.mapper;

import com.chatwoot.api.inbox.dto.InboxResponse;
import com.chatwoot.api.inbox.model.Inbox;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InboxMapper {

    public InboxResponse inbox(Inbox inbox) {
        return new InboxResponse(
                inbox.getId(),
                "",
                inbox.getChannelId(),
                inbox.getName(),
                inbox.getChannelType(),
                inbox.getGreetingEnabled(),
                inbox.getGreetingMessage(),
                inbox.getWorkingHoursEnabled(),
                inbox.getEnableEmailCollect(),
                inbox.getCsatSurveyEnabled(),
                inbox.getCsatConfig(),
                inbox.getEnableAutoAssignment(),
                inbox.getAutoAssignmentConfig(),
                inbox.getOutOfOfficeMessage(),
                List.of(),
                inbox.getTimezone(),
                inbox.getAllowMessagesAfterResolved(),
                inbox.isLockToSingleConversation(),
                inbox.senderNameTypeName(),
                inbox.getBusinessName()
        );
    }
}
