package com.chatwoot.api.integration.facebook.service;

import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.inbox.repository.InboxRepository;
import com.chatwoot.api.integration.facebook.builder.FacebookMessageBuilder;
import com.chatwoot.api.integration.facebook.config.FacebookProperties;
import com.chatwoot.api.integration.facebook.model.FacebookPage;
import com.chatwoot.api.integration.facebook.repository.FacebookPageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FacebookWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(FacebookWebhookProcessor.class);

    private final FacebookPageRepository facebookPages;
    private final InboxRepository inboxes;
    private final FacebookMessageBuilder messageBuilder;
    private final FacebookProperties properties;

    public FacebookWebhookProcessor(
            FacebookPageRepository facebookPages,
            InboxRepository inboxes,
            FacebookMessageBuilder messageBuilder,
            FacebookProperties properties
    ) {
        this.facebookPages = facebookPages;
        this.inboxes = inboxes;
        this.messageBuilder = messageBuilder;
        this.properties = properties;
    }

    public void process(JsonNode payload) {
        if (payload == null || !payload.path("entry").isArray()) {
            return;
        }
        for (JsonNode entry : payload.path("entry")) {
            processBatch(entry.path("messaging"));
            processBatch(entry.path("standby"));
        }
    }

    private void processBatch(JsonNode events) {
        if (!events.isArray()) {
            return;
        }
        for (JsonNode raw : events) {
            FacebookMessagingEvent event = new FacebookMessagingEvent(raw);
            if (event.deliveryOrRead()) {
                continue;
            }
            try {
                if (event.echo()) {
                    if (event.sentFromApp(properties.appId())) {
                        continue;
                    }
                    createAgentMessage(event);
                } else {
                    createContactMessage(event);
                }
            } catch (RuntimeException ex) {
                log.error("Error processing Facebook webhook event: {}", ex.getMessage());
            }
        }
    }

    private void createContactMessage(FacebookMessagingEvent event) {
        for (FacebookPage page : facebookPages.findByPageId(event.recipientId())) {
            Inbox inbox = inboxFor(page);
            if (inbox != null) {
                messageBuilder.perform(event, inbox, page, false);
            }
        }
    }

    private void createAgentMessage(FacebookMessagingEvent event) {
        for (FacebookPage page : facebookPages.findByPageId(event.senderId())) {
            Inbox inbox = inboxFor(page);
            if (inbox != null) {
                messageBuilder.perform(event, inbox, page, true);
            }
        }
    }

    private Inbox inboxFor(FacebookPage page) {
        return inboxes.findByChannelTypeAndChannelId(Inbox.CHANNEL_FACEBOOK, page.getId()).orElse(null);
    }
}
