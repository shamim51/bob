package com.bob.api.contact.repository;

import com.bob.api.contact.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    Optional<Contact> findByIdAndAccountId(Integer id, Integer accountId);
}
