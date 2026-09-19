package com.bob.api.contact.repository;

import com.bob.api.contact.model.ContactInbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactInboxRepository extends JpaRepository<ContactInbox, Long> {
    Optional<ContactInbox> findByInbox_IdAndSourceId(Integer inboxId, String sourceId);
}
