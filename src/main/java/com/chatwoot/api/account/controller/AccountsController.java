package com.chatwoot.api.account.controller;

import com.chatwoot.api.account.dto.AccountResponse;
import com.chatwoot.api.account.dto.CacheKeysEnvelope;
import com.chatwoot.api.account.mapper.AccountMapper;
import com.chatwoot.api.account.model.Account;
import com.chatwoot.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class AccountsController {

    private final CurrentUserService currentUserService;
    private final AccountMapper mapper;

    public AccountsController(CurrentUserService currentUserService, AccountMapper mapper) {
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    @GetMapping({"", "/"})
    public AccountResponse show(@PathVariable Integer accountId) {
        Account account = currentUserService.requireMembership(accountId).getAccount();
        return mapper.account(account);
    }

    @GetMapping("/cache_keys")
    public CacheKeysEnvelope cacheKeys(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return new CacheKeysEnvelope(AccountResponse.CacheKeysResponse.zeros());
    }
}
