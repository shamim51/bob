package com.chatwoot.api.account.mapper;

import com.chatwoot.api.account.dto.AccountResponse;
import com.chatwoot.api.account.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse account(Account account) {
        return new AccountResponse(
                account.getSettings(),
                account.getCreatedAt(),
                account.getCustomAttributes(),
                account.getDomain(),
                AccountResponse.AccountFeaturesResponse.defaults(),
                account.getId(),
                account.localeCode(),
                account.getName(),
                account.getSupportEmail(),
                account.statusName(),
                AccountResponse.CacheKeysResponse.zeros()
        );
    }
}
