package com.bob.api.messaging.mapper;

import com.bob.api.account.mapper.AgentMapper;
import com.bob.api.contact.mapper.ContactMapper;
import com.bob.api.conversation.model.Conversation;
import com.bob.api.messaging.dto.MessageResponse;
import com.bob.api.messaging.model.Message;
import com.bob.api.shared.dto.ChatwootTimestamps;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    private final AgentMapper agents;
    private final ContactMapper contacts;

    public MessageMapper(AgentMapper agents, ContactMapper contacts) {
        this.agents = agents;
        this.contacts = contacts;
    }

    public MessageResponse message(Message message) {
        Conversation conversation = message.getConversation();
        return message(message, conversation == null ? null : conversation.getDisplayId());
    }

    public MessageResponse message(Message message, Integer conversationDisplayId) {
        return new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getInboxId(),
                message.getEchoId(),
                conversationDisplayId,
                message.getMessageType(),
                message.contentTypeName(),
                message.statusName(),
                message.getContentAttributes(),
                ChatwootTimestamps.unix(message.getCreatedAt()),
                message.isPrivateMessage(),
                message.getSourceId(),
                sender(message)
        );
    }

    private Object sender(Message message) {
        if ("User".equals(message.getSenderType()) && message.getSenderUser() != null) {
            return agents.agent(message.getSenderUser(), message.getAccountId());
        }
        if ("Contact".equals(message.getSenderType()) && message.getSenderContact() != null) {
            return contacts.contact(message.getSenderContact(), false);
        }
        return null;
    }
}
