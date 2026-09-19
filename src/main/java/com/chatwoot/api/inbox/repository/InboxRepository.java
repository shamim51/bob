package com.chatwoot.api.inbox.repository;

import com.chatwoot.api.inbox.model.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InboxRepository extends JpaRepository<Inbox, Integer> {
    List<Inbox> findByAccountId(Integer accountId);

    Optional<Inbox> findByIdAndAccountId(Integer id, Integer accountId);

    Optional<Inbox> findByChannelTypeAndChannelId(String channelType, Integer channelId);
}
