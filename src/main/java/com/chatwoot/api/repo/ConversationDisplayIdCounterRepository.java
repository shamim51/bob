package com.chatwoot.api.repo;

import com.chatwoot.api.domain.ConversationDisplayIdCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ConversationDisplayIdCounterRepository extends JpaRepository<ConversationDisplayIdCounter, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConversationDisplayIdCounter> findByAccountId(Integer accountId);
}
