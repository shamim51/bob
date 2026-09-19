package com.bob.api.contact.mapper;

import com.bob.api.contact.dto.ContactResponse;
import com.bob.api.contact.model.Contact;
import com.bob.api.shared.dto.ChatwootTimestamps;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

    public ContactResponse contact(Contact contact, boolean includeTimestamps) {
        return new ContactResponse(
                contact.getAdditionalAttributes(),
                "offline",
                contact.getEmail(),
                contact.getId(),
                contact.getName(),
                contact.getPhoneNumber(),
                contact.isBlocked(),
                contact.getIdentifier(),
                "",
                contact.getCustomAttributes(),
                "contact",
                includeTimestamps ? ChatwootTimestamps.unix(contact.getLastActivityAt()) : null,
                includeTimestamps ? ChatwootTimestamps.unix(contact.getCreatedAt()) : null
        );
    }
}
