package com.chatwoot.api.contact.repository;

import com.chatwoot.api.contact.model.ContactInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactInboxRepository extends JpaRepository<ContactInbox, Long> {
}
