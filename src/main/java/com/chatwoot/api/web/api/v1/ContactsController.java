package com.chatwoot.api.web.api.v1;

import com.chatwoot.api.domain.Contact;
import com.chatwoot.api.repo.ContactRepository;
import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.web.dto.ChatwootJson;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/contacts")
public class ContactsController {

    private final CurrentUserService currentUserService;
    private final ContactRepository contacts;
    private final ChatwootJson json;

    public ContactsController(CurrentUserService currentUserService, ContactRepository contacts, ChatwootJson json) {
        this.currentUserService = currentUserService;
        this.contacts = contacts;
        this.json = json;
    }

    @GetMapping("/{contactId}")
    public Map<String, Object> show(@PathVariable Integer accountId, @PathVariable Integer contactId) {
        currentUserService.requireMembership(accountId);
        Contact contact = contacts.findByIdAndAccountId(contactId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Map.of("payload", json.contact(contact, true));
    }
}
