package com.chatwoot.api.messaging.mapper;

import com.chatwoot.api.messaging.model.Message;
import com.chatwoot.api.contact.repository.ContactRepository;
import com.chatwoot.api.account.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageHydrator {

    private final UserRepository users;
    private final ContactRepository contacts;

    public MessageHydrator(UserRepository users, ContactRepository contacts) {
        this.users = users;
        this.contacts = contacts;
    }

    public void hydrate(List<Message> messages) {
        messages.forEach(this::hydrate);
    }

    public void hydrate(Message message) {
        if ("User".equals(message.getSenderType()) && message.getSenderId() != null) {
            users.findById(message.getSenderId().intValue()).ifPresent(message::setSenderUser);
        }
        if ("Contact".equals(message.getSenderType()) && message.getSenderId() != null) {
            contacts.findById(message.getSenderId().intValue()).ifPresent(message::setSenderContact);
        }
    }
}
