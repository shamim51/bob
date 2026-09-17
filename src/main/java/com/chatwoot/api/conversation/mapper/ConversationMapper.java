package com.chatwoot.api.conversation.mapper;

import com.chatwoot.api.account.dto.AgentResponse;
import com.chatwoot.api.account.mapper.AgentMapper;
import com.chatwoot.api.contact.dto.ContactResponse;
import com.chatwoot.api.contact.mapper.ContactMapper;
import com.chatwoot.api.conversation.dto.ConversationResponse;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.messaging.dto.MessageResponse;
import com.chatwoot.api.messaging.mapper.MessageMapper;
import com.chatwoot.api.messaging.model.Message;
import com.chatwoot.api.messaging.repository.MessageRepository;
import com.chatwoot.api.shared.dto.ChatwootTimestamps;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ConversationMapper {

    private final MessageRepository messages;
    private final ContactMapper contacts;
    private final AgentMapper agents;
    private final MessageMapper messageMapper;

    public ConversationMapper(
            MessageRepository messages,
            ContactMapper contacts,
            AgentMapper agents,
            MessageMapper messageMapper
    ) {
        this.messages = messages;
        this.contacts = contacts;
        this.agents = agents;
        this.messageMapper = messageMapper;
    }

    public ConversationResponse conversation(Conversation conversation) {
        ContactResponse sender = conversation.getContact() == null
                ? null
                : contacts.contact(conversation.getContact(), false);
        AgentResponse assignee = conversation.getAssignee() == null
                ? null
                : agents.agent(conversation.getAssignee(), conversation.getAccountId());
        String assigneeType = conversation.getAssignee() == null ? null : "User";
        var meta = new ConversationResponse.ConversationMetaResponse(
                sender,
                conversation.getInbox() == null ? null : conversation.getInbox().getChannelType(),
                assignee,
                assigneeType,
                conversation.getContactInbox() != null && conversation.getContactInbox().isHmacVerified()
        );

        List<Message> latest = messages.findLatestFirst(conversation.getId());
        List<MessageResponse> embedded = latest.isEmpty()
                ? List.of()
                : List.of(messageMapper.message(latest.getFirst()));

        List<Message> nonActivity = messages.findNonActivityDesc(conversation.getId(), conversation.getAccountId());
        MessageResponse lastNonActivity = nonActivity.isEmpty() ? null : messageMapper.message(nonActivity.getFirst());

        return new ConversationResponse(
                meta,
                conversation.getDisplayId(),
                embedded,
                conversation.getAccountId(),
                conversation.getUuid() == null ? null : conversation.getUuid().toString(),
                conversation.getAdditionalAttributes(),
                ChatwootTimestamps.unix(conversation.getAgentLastSeenAt()),
                ChatwootTimestamps.unix(conversation.getAssigneeLastSeenAt()),
                canReply(conversation),
                ConversationResponse.ContactInfoRequestResponse.unavailable(),
                ChatwootTimestamps.unix(conversation.getContactLastSeenAt()),
                conversation.getCustomAttributes(),
                conversation.getInboxId(),
                conversation.labelList(),
                conversation.muted(),
                conversation.getSnoozedUntil(),
                conversation.statusName(),
                ChatwootTimestamps.unix(conversation.getCreatedAt()),
                ChatwootTimestamps.unixFloat(conversation.getUpdatedAt()),
                ChatwootTimestamps.unix(conversation.getLastActivityAt()),
                ChatwootTimestamps.unix(conversation.getFirstReplyCreatedAt()),
                unreadCount(conversation),
                lastNonActivity,
                ChatwootTimestamps.unix(conversation.getLastActivityAt()),
                conversation.priorityName(),
                ChatwootTimestamps.unix(conversation.getWaitingSince()),
                null
        );
    }

    private boolean canReply(Conversation conversation) {
        Inbox inbox = conversation.getInbox();
        if (inbox == null || inbox.apiChannel()) {
            return true;
        }
        List<Message> incoming = messages.findIncomingAsc(conversation.getId(), conversation.getAccountId());
        if (incoming.isEmpty()) {
            return false;
        }
        Instant last = incoming.getLast().getCreatedAt();
        return Instant.now().isBefore(last.plus(Duration.ofHours(24)));
    }

    private long unreadCount(Conversation conversation) {
        return messages.countUnreadIncoming(conversation.getId(), conversation.getAgentLastSeenAt());
    }
}
