package com.chatwoot.api.repo;

import com.chatwoot.api.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    Optional<Contact> findByIdAndAccountId(Integer id, Integer accountId);
}
