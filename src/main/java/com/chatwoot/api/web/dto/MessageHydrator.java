package com.chatwoot.api.web.dto;

import com.chatwoot.api.domain.Message;
import com.chatwoot.api.repo.ContactRepository;
import com.chatwoot.api.repo.UserRepository;
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
