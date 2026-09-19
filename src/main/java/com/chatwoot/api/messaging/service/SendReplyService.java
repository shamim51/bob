package com.chatwoot.api.messaging.service;

import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.integration.facebook.service.SendOnFacebookService;
import com.chatwoot.api.messaging.model.Message;
import org.springframework.stereotype.Service;

@Service
public class SendReplyService {

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
        if (inbox != null && inbox.facebookChannel()) {
            facebook.perform(message);
        }
    }
}
