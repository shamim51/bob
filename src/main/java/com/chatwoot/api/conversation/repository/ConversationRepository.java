package com.chatwoot.api.conversation.repository;

import com.chatwoot.api.conversation.model.Conversation;
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

    Optional<Conversation> findFirstByInbox_IdAndContact_IdOrderByCreatedAtDesc(Integer inboxId, Integer contactId);

    Optional<Conversation> findFirstByInbox_IdAndContact_IdAndStatusNotOrderByCreatedAtDesc(
            Integer inboxId,
            Integer contactId,
            Integer status
    );
}
