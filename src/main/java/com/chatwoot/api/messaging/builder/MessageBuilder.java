package com.chatwoot.api.messaging.builder;

import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.messaging.model.Message;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.conversation.repository.ConversationRepository;
import com.chatwoot.api.messaging.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of Chatwoot Messages::MessageBuilder (persist + last_activity_at). Channel send is skipped.
 */
@Service
public class MessageBuilder {

    private final MessageRepository messages;
    private final ConversationRepository conversations;

    public MessageBuilder(MessageRepository messages, ConversationRepository conversations) {
        this.messages = messages;
        this.conversations = conversations;
    }

    @Transactional
    public Message perform(User user, Conversation conversation, CreateMessageParams params) {
        String typeName = params.messageType() == null ? "outgoing" : params.messageType();
        if (!conversation.getInbox().apiChannel() && "incoming".equals(typeName)) {
            throw new IllegalArgumentException("Incoming messages are only allowed in Api inboxes");
        }
        int messageType = Message.messageTypeFromName(typeName);
        Message message = new Message();
        message.setConversation(conversation);
        message.setAccountId(conversation.getAccountId());
        message.setInboxId(conversation.getInboxId());
        message.setMessageType(messageType);
        message.setContent(params.content());
        message.setPrivateMessage(Boolean.TRUE.equals(params.privateNote()));
        message.setContentType(Message.contentTypeFromName(params.contentType()));
        message.setContentAttributes(params.contentAttributes() == null ? new HashMap<>() : new HashMap<>(params.contentAttributes()));
        message.setSourceId(params.sourceId());
        message.setEchoId(params.echoId());
        if (messageType == Message.TYPE_OUTGOING) {
            message.setSenderType("User");
            message.setSenderId(user.getId().longValue());
            message.setSenderUser(user);
        } else if (messageType == Message.TYPE_INCOMING) {
            message.setSenderType("Contact");
            if (conversation.getContact() != null) {
                message.setSenderId(conversation.getContact().getId().longValue());
                message.setSenderContact(conversation.getContact());
            }
        }
        Message saved = messages.save(message);
        saved.setEchoId(params.echoId());
        conversation.setLastActivityAt(saved.getCreatedAt());
        if (saved.getMessageType() == Message.TYPE_OUTGOING
                && !saved.isPrivateMessage()
                && conversation.getFirstReplyCreatedAt() == null) {
            conversation.setFirstReplyCreatedAt(saved.getCreatedAt());
        }
        conversations.save(conversation);
        return saved;
    }

    public record CreateMessageParams(
            String content,
            Boolean privateNote,
            String echoId,
            String messageType,
            String contentType,
            Map<String, Object> contentAttributes,
            String sourceId
    ) {
    }
}
