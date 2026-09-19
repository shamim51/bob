package com.chatwoot.api.integration.facebook.builder;

import com.chatwoot.api.contact.model.Contact;
import com.chatwoot.api.contact.model.ContactInbox;
import com.chatwoot.api.contact.repository.ContactInboxRepository;
import com.chatwoot.api.contact.repository.ContactRepository;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.conversation.repository.ConversationRepository;
import com.chatwoot.api.conversation.service.ConversationDisplayIdService;
import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.integration.facebook.model.FacebookPage;
import com.chatwoot.api.integration.facebook.service.FacebookGraphClient;
import com.chatwoot.api.integration.facebook.service.FacebookMessagingEvent;
import com.chatwoot.api.messaging.model.Message;
import com.chatwoot.api.messaging.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class FacebookMessageBuilder {

    private final ContactRepository contacts;
    private final ContactInboxRepository contactInboxes;
    private final ConversationRepository conversations;
    private final ConversationDisplayIdService displayIds;
    private final MessageRepository messages;
    private final FacebookGraphClient graph;

    public FacebookMessageBuilder(
            ContactRepository contacts,
            ContactInboxRepository contactInboxes,
            ConversationRepository conversations,
            ConversationDisplayIdService displayIds,
            MessageRepository messages,
            FacebookGraphClient graph
    ) {
        this.contacts = contacts;
        this.contactInboxes = contactInboxes;
        this.conversations = conversations;
        this.displayIds = displayIds;
        this.messages = messages;
        this.graph = graph;
    }

    @Transactional
    public Message perform(FacebookMessagingEvent event, Inbox inbox, FacebookPage page, boolean outgoingEcho) {
        if (page.isReauthorizationRequired()) {
            return null;
        }
        String senderId = outgoingEcho ? event.recipientId() : event.senderId();
        if (senderId == null) {
            return null;
        }
        String content = event.content();
        if (content == null && event.identifier() == null) {
            return null;
        }
        ContactInbox contactInbox = findOrCreateContactInbox(inbox, page, senderId);
        Conversation conversation = findOrCreateConversation(inbox, contactInbox);
        Message message = new Message();
        message.setConversation(conversation);
        message.setAccountId(conversation.getAccountId());
        message.setInboxId(inbox.getId());
        message.setMessageType(outgoingEcho ? Message.TYPE_OUTGOING : Message.TYPE_INCOMING);
        message.setStatus(outgoingEcho ? Message.STATUS_DELIVERED : Message.STATUS_SENT);
        message.setContent(content);
        message.setSourceId(event.identifier());
        Map<String, Object> attributes = new HashMap<>();
        if (event.inReplyToExternalId() != null) {
            attributes.put("in_reply_to_external_id", event.inReplyToExternalId());
        }
        if (outgoingEcho) {
            attributes.put("external_echo", true);
        }
        message.setContentAttributes(attributes);
        if (!outgoingEcho) {
            message.setSenderType("Contact");
            message.setSenderId(contactInbox.getContact().getId().longValue());
            message.setSenderContact(contactInbox.getContact());
        }
        Message saved = messages.save(message);
        conversation.setLastActivityAt(saved.getCreatedAt());
        conversations.save(conversation);
        return saved;
    }

    private ContactInbox findOrCreateContactInbox(Inbox inbox, FacebookPage page, String sourceId) {
        return contactInboxes.findByInbox_IdAndSourceId(inbox.getId(), sourceId)
                .orElseGet(() -> createContactInbox(inbox, page, sourceId));
    }

    private ContactInbox createContactInbox(Inbox inbox, FacebookPage page, String sourceId) {
        FacebookGraphClient.FacebookUserProfile profile = graph.fetchUserProfile(page.getPageAccessToken(), sourceId);
        Contact contact = new Contact();
        contact.setAccountId(inbox.getAccountId());
        contact.setName(profile.displayName());
        contact = contacts.save(contact);
        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId(sourceId);
        return contactInboxes.save(contactInbox);
    }

    private Conversation findOrCreateConversation(Inbox inbox, ContactInbox contactInbox) {
        Integer contactId = contactInbox.getContact().getId();
        if (inbox.isLockToSingleConversation()) {
            return conversations.findFirstByInbox_IdAndContact_IdOrderByCreatedAtDesc(inbox.getId(), contactId)
                    .orElseGet(() -> buildConversation(inbox, contactInbox));
        }
        return conversations.findFirstByInbox_IdAndContact_IdAndStatusNotOrderByCreatedAtDesc(
                        inbox.getId(), contactId, Conversation.STATUS_RESOLVED)
                .orElseGet(() -> buildConversation(inbox, contactInbox));
    }

    private Conversation buildConversation(Inbox inbox, ContactInbox contactInbox) {
        Conversation conversation = new Conversation();
        conversation.setAccountId(inbox.getAccountId());
        conversation.setInbox(inbox);
        conversation.setContact(contactInbox.getContact());
        conversation.setContactInbox(contactInbox);
        conversation.setStatus(Conversation.STATUS_OPEN);
        conversation.setDisplayId(displayIds.next(inbox.getAccountId()));
        return conversations.save(conversation);
    }
}
