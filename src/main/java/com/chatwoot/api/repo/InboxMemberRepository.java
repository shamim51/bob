package com.chatwoot.api.repo;

import com.chatwoot.api.domain.InboxMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InboxMemberRepository extends JpaRepository<InboxMember, Integer> {
    List<InboxMember> findByUserId(Integer userId);
}
