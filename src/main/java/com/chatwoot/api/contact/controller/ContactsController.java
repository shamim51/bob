package com.chatwoot.api.contact.controller;

import com.chatwoot.api.contact.dto.ContactResponse;
import com.chatwoot.api.contact.mapper.ContactMapper;
import com.chatwoot.api.contact.model.Contact;
import com.chatwoot.api.contact.repository.ContactRepository;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.shared.dto.PayloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/contacts")
public class ContactsController {

    private final CurrentUserService currentUserService;
    private final ContactRepository contacts;
    private final ContactMapper mapper;

    public ContactsController(
            CurrentUserService currentUserService,
            ContactRepository contacts,
            ContactMapper mapper
    ) {
        this.currentUserService = currentUserService;
        this.contacts = contacts;
        this.mapper = mapper;
    }

    @GetMapping("/{contactId}")
    public PayloadResponse<ContactResponse> show(@PathVariable Integer accountId, @PathVariable Integer contactId) {
        currentUserService.requireMembership(accountId);
        Contact contact = contacts.findByIdAndAccountId(contactId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new PayloadResponse<>(mapper.contact(contact, true));
    }
}
