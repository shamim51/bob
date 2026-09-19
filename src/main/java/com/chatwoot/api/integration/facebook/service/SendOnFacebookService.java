package com.chatwoot.api.integration.facebook.service;

import com.chatwoot.api.contact.model.ContactInbox;
import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.integration.facebook.model.FacebookPage;
import com.chatwoot.api.integration.facebook.repository.FacebookPageRepository;
import com.chatwoot.api.messaging.model.Message;
import com.chatwoot.api.messaging.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SendOnFacebookService {

    private static final Logger log = LoggerFactory.getLogger(SendOnFacebookService.class);

    private final FacebookPageRepository facebookPages;
    private final FacebookGraphClient graph;
    private final MessageRepository messages;

    public SendOnFacebookService(
            FacebookPageRepository facebookPages,
            FacebookGraphClient graph,
            MessageRepository messages
    ) {
        this.facebookPages = facebookPages;
        this.graph = graph;
        this.messages = messages;
    }

    public void perform(Message message) {
        Inbox inbox = message.getConversation().getInbox();
        if (inbox == null || !inbox.facebookChannel()) {
            return;
        }
        FacebookPage page = facebookPages.findById(inbox.getChannelId()).orElse(null);
        if (page == null) {
            return;
        }
        ContactInbox contactInbox = message.getConversation().getContactInbox();
        if (contactInbox == null || contactInbox.getSourceId() == null) {
            return;
        }
        String text = message.getContent();
        if (text == null || text.isBlank()) {
            return;
        }
        FacebookGraphClient.FacebookSendResult result = graph.sendText(
                page.getPageAccessToken(),
                contactInbox.getSourceId(),
                text
        );
        if (result.failed()) {
            log.info("FB_GRAPH action=send_on_facebook result=FAILED pageId={} message={}",
                    page.getPageId(), result.errorMessage());
            message.setStatus(Message.STATUS_FAILED);
            if (result.authorizationError()) {
                page.setReauthorizationRequired(true);
                facebookPages.save(page);
            }
            messages.save(message);
            return;
        }
        if (result.messageId() != null) {
            message.setSourceId(result.messageId());
            messages.save(message);
            log.info("FB_GRAPH action=send_on_facebook result=SUCCESS pageId={} conversationDisplayId={}",
                    page.getPageId(), message.getConversation().getDisplayId());
        }
    }
}
