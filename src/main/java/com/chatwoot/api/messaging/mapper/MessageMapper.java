package com.chatwoot.api.messaging.mapper;

import com.chatwoot.api.account.mapper.AgentMapper;
import com.chatwoot.api.contact.mapper.ContactMapper;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.messaging.dto.MessageResponse;
import com.chatwoot.api.messaging.model.Message;
import com.chatwoot.api.shared.dto.ChatwootTimestamps;
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
        return new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getInboxId(),
                message.getEchoId(),
                conversation == null ? null : conversation.getDisplayId(),
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
