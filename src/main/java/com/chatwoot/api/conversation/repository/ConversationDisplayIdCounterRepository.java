package com.chatwoot.api.conversation.repository;

import com.chatwoot.api.conversation.model.ConversationDisplayIdCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ConversationDisplayIdCounterRepository extends JpaRepository<ConversationDisplayIdCounter, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConversationDisplayIdCounter> findByAccountId(Integer accountId);
}
