package com.chatwoot.api.account.repository;

import com.chatwoot.api.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Integer> {
}
