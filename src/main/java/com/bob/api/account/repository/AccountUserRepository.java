package com.bob.api.account.repository;

import com.bob.api.account.model.AccountUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {
    @EntityGraph(attributePaths = {"account", "user"})
    Optional<AccountUser> findByAccount_IdAndUser_Id(Integer accountId, Integer userId);

    @EntityGraph(attributePaths = "user")
    List<AccountUser> findByAccount_Id(Integer accountId);

    @EntityGraph(attributePaths = "account")
    List<AccountUser> findByUser_Id(Integer userId);
}
