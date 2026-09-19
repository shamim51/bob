package com.chatwoot.api.messaging.service;

import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.integration.facebook.service.SendOnFacebookService;
import com.chatwoot.api.messaging.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SendReplyService {

    private static final Logger log = LoggerFactory.getLogger(SendReplyService.class);

    private final SendOnFacebookService facebook;

    public SendReplyService(SendOnFacebookService facebook) {
        this.facebook = facebook;
    }

    public void perform(Message message) {
        if (message.isPrivateMessage()) {
            return;
        }
        if (message.getSourceId() != null && !message.getSourceId().isBlank()) {
            return;
        }
        if (message.getMessageType() != Message.TYPE_OUTGOING && message.getMessageType() != Message.TYPE_TEMPLATE) {
            return;
        }
        Inbox inbox = message.getConversation() == null ? null : message.getConversation().getInbox();
        Integer displayId = message.getConversation() == null ? null : message.getConversation().getDisplayId();
        if (inbox != null && inbox.facebookChannel()) {
            log.info("MESSAGE action=send_reply channel=facebook conversationDisplayId={}", displayId);
            facebook.perform(message);
            return;
        }
        log.info("MESSAGE action=send_reply channel=api conversationDisplayId={}", displayId);
    }
}
