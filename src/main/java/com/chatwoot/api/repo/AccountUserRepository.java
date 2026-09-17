package com.chatwoot.api.repo;

import com.chatwoot.api.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {
    Optional<AccountUser> findByAccount_IdAndUser_Id(Integer accountId, Integer userId);

    List<AccountUser> findByAccount_Id(Integer accountId);

    List<AccountUser> findByUser_Id(Integer userId);
}
