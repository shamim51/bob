package com.chatwoot.api.inbox.repository;

import com.chatwoot.api.inbox.model.InboxMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InboxMemberRepository extends JpaRepository<InboxMember, Integer> {
    List<InboxMember> findByUserId(Integer userId);

    List<InboxMember> findByInboxId(Integer inboxId);

    void deleteByInboxIdAndUserIdIn(Integer inboxId, List<Integer> userIds);
}
