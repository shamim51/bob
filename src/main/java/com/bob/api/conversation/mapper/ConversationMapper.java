package com.bob.api.conversation.mapper;

import com.bob.api.account.dto.AgentResponse;
import com.bob.api.account.mapper.AgentMapper;
import com.bob.api.contact.dto.ContactResponse;
import com.bob.api.contact.mapper.ContactMapper;
import com.bob.api.conversation.dto.ConversationResponse;
import com.bob.api.conversation.model.Conversation;
import com.bob.api.inbox.model.Inbox;
import com.bob.api.messaging.dto.MessageResponse;
import com.bob.api.messaging.mapper.MessageHydrator;
import com.bob.api.messaging.mapper.MessageMapper;
import com.bob.api.messaging.model.Message;
import com.bob.api.messaging.repository.MessageRepository;
import com.bob.api.shared.dto.ChatwootTimestamps;
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
    private final MessageHydrator hydrator;

    public ConversationMapper(
            MessageRepository messages,
            ContactMapper contacts,
            AgentMapper agents,
            MessageMapper messageMapper,
            MessageHydrator hydrator
    ) {
        this.messages = messages;
        this.contacts = contacts;
        this.agents = agents;
        this.messageMapper = messageMapper;
        this.hydrator = hydrator;
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

        Integer displayId = conversation.getDisplayId();
        List<Message> latest = messages.findLatestFirst(conversation.getId());
        List<MessageResponse> embedded = List.of();
        if (!latest.isEmpty()) {
            Message latestMessage = latest.getFirst();
            hydrator.hydrate(latestMessage);
            embedded = List.of(messageMapper.message(latestMessage, displayId));
        }

        List<Message> nonActivity = messages.findNonActivityDesc(conversation.getId(), conversation.getAccountId());
        MessageResponse lastNonActivity = null;
        if (!nonActivity.isEmpty()) {
            Message last = nonActivity.getFirst();
            hydrator.hydrate(last);
            lastNonActivity = messageMapper.message(last, displayId);
        }

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
        Instant since = conversation.getAgentLastSeenAt();
        if (since == null) {
            return messages.countUnreadIncoming(conversation.getId());
        }
        return messages.countUnreadIncomingSince(conversation.getId(), since);
    }
}
