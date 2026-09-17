package com.chatwoot.api.repo;

import com.chatwoot.api.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    @Query("""
            select c from Conversation c
            join fetch c.inbox
            left join fetch c.contact
            left join fetch c.assignee
            left join fetch c.contactInbox
            where c.accountId = :accountId and c.displayId = :displayId
            """)
    Optional<Conversation> findByAccountIdAndDisplayId(@Param("accountId") Integer accountId, @Param("displayId") Integer displayId);
}
