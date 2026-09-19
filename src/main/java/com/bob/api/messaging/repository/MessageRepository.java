package com.bob.api.messaging.repository;

import com.bob.api.messaging.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    @Query("""
            select m from Message m
            where m.conversation.id = :conversationPk
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findLatestFirst(@Param("conversationPk") Integer conversationPk);

    @Query("""
            select m from Message m
            where m.conversation.id = :conversationPk
              and m.accountId = :accountId
              and m.messageType = 0
            order by m.createdAt asc, m.id asc
            """)
    List<Message> findIncomingAsc(@Param("conversationPk") Integer conversationPk, @Param("accountId") Integer accountId);

    @Query("""
            select count(m) from Message m
            where m.conversation.id = :conversationPk
              and m.messageType = 0
              and (:since is null or m.createdAt > :since)
            """)
    long countUnreadIncoming(@Param("conversationPk") Integer conversationPk, @Param("since") java.time.Instant since);

    @Query("""
            select m from Message m
            where m.conversation.id = :conversationPk
              and m.accountId = :accountId
              and m.messageType <> 2
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findNonActivityDesc(@Param("conversationPk") Integer conversationPk, @Param("accountId") Integer accountId);

    Optional<Message> findByIdAndConversation_Id(Integer id, Integer conversationPk);
}
