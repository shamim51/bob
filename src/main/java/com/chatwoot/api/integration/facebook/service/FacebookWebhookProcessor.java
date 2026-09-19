package com.chatwoot.api.integration.facebook.service;

import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.inbox.repository.InboxRepository;
import com.chatwoot.api.integration.facebook.builder.FacebookMessageBuilder;
import com.chatwoot.api.integration.facebook.config.FacebookProperties;
import com.chatwoot.api.integration.facebook.model.FacebookPage;
import com.chatwoot.api.integration.facebook.repository.FacebookPageRepository;
import com.chatwoot.api.messaging.model.Message;
import tools.jackson.databind.JsonNode;
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
                        log.info("FB_WEBHOOK action=skip_echo result=SKIPPED reason=own_app");
                        continue;
                    }
                    createAgentMessage(event);
                } else {
                    createContactMessage(event);
                }
            } catch (RuntimeException ex) {
                log.error("FB_WEBHOOK action=process_event result=FAILED message={}", ex.getMessage());
            }
        }
    }

    private void createContactMessage(FacebookMessagingEvent event) {
        for (FacebookPage page : facebookPages.findByPageId(event.recipientId())) {
            persistWebhookMessage(event, page, false);
        }
    }

    private void createAgentMessage(FacebookMessagingEvent event) {
        for (FacebookPage page : facebookPages.findByPageId(event.senderId())) {
            persistWebhookMessage(event, page, true);
        }
    }

    private void persistWebhookMessage(FacebookMessagingEvent event, FacebookPage page, boolean outgoingEcho) {
        Inbox inbox = inboxFor(page);
        if (inbox == null) {
            log.info("FB_WEBHOOK action={} result=SKIPPED reason=inbox_not_found pageId={}",
                    outgoingEcho ? "echo_message" : "inbound_message", page.getPageId());
            return;
        }
        Message saved = messageBuilder.perform(event, inbox, page, outgoingEcho);
        if (saved == null) {
            log.info("FB_WEBHOOK action={} result=SKIPPED pageId={}",
                    outgoingEcho ? "echo_message" : "inbound_message", page.getPageId());
            return;
        }
        Integer displayId = saved.getConversation() == null ? null : saved.getConversation().getDisplayId();
        log.info("FB_WEBHOOK action={} result=SAVED pageId={} conversationDisplayId={}",
                outgoingEcho ? "echo_message" : "inbound_message", page.getPageId(), displayId);
    }

    private Inbox inboxFor(FacebookPage page) {
        return inboxes.findByChannelTypeAndChannelId(Inbox.CHANNEL_FACEBOOK, page.getId()).orElse(null);
    }
}
