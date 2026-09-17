package com.chatwoot.api.account.controller;

import com.chatwoot.api.account.dto.AgentResponse;
import com.chatwoot.api.account.mapper.AgentMapper;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.account.repository.AccountUserRepository;
import com.chatwoot.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class AgentsController {

    private final CurrentUserService currentUserService;
    private final AccountUserRepository accountUsers;
    private final AgentMapper mapper;

    public AgentsController(
            CurrentUserService currentUserService,
            AccountUserRepository accountUsers,
            AgentMapper mapper
    ) {
        this.currentUserService = currentUserService;
        this.accountUsers = accountUsers;
        this.mapper = mapper;
    }

    @GetMapping("/agents")
    public List<AgentResponse> agents(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return accountUsers.findByAccount_Id(accountId).stream()
                .map(membership -> {
                    User user = membership.getUser();
                    user.setCurrentAccountUser(membership);
                    return mapper.agent(user, accountId);
                })
                .toList();
    }
}
