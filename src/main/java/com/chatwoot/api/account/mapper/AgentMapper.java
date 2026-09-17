package com.chatwoot.api.account.mapper;

import com.chatwoot.api.account.dto.AgentResponse;
import com.chatwoot.api.account.model.User;
import org.springframework.stereotype.Component;

@Component
public class AgentMapper {

    public AgentResponse agent(User user, Integer accountId) {
        return new AgentResponse(
                user.getId(),
                accountId,
                user.availabilityStatus(),
                user.getCurrentAccountUser() == null || user.getCurrentAccountUser().isAutoOffline(),
                true,
                user.getEmail(),
                user.getProvider(),
                user.availableName(),
                user.getName(),
                user.getCurrentAccountUser() == null ? "agent" : user.getCurrentAccountUser().roleName(),
                "",
                "user"
        );
    }
}
