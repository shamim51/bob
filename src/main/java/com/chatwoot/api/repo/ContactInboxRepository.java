package com.chatwoot.api.repo;

import com.chatwoot.api.domain.ContactInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactInboxRepository extends JpaRepository<ContactInbox, Long> {
}
