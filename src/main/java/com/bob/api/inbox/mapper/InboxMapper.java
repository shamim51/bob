package com.bob.api.inbox.mapper;

import com.bob.api.inbox.dto.InboxResponse;
import com.bob.api.inbox.model.Inbox;
import com.bob.api.integration.facebook.model.FacebookPage;
import com.bob.api.integration.facebook.repository.FacebookPageRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InboxMapper {

    private final FacebookPageRepository facebookPages;

    public InboxMapper(FacebookPageRepository facebookPages) {
        this.facebookPages = facebookPages;
    }

    public InboxResponse inbox(Inbox inbox) {
        FacebookPage page = inbox.facebookChannel()
                ? facebookPages.findById(inbox.getChannelId()).orElse(null)
                : null;
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
                inbox.getBusinessName(),
                page == null ? null : page.getPageId(),
                page == null ? null : page.getProviderName(),
                page == null ? null : page.isReauthorizationRequired()
        );
    }
}
