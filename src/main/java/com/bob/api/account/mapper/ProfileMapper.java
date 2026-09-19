package com.bob.api.account.mapper;

import com.bob.api.account.dto.UserProfileResponse;
import com.bob.api.account.model.AccountUser;
import com.bob.api.account.model.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProfileMapper {

    @Transactional(readOnly = true)
    public UserProfileResponse userProfile(User user, List<AccountUser> memberships) {
        AccountUser active = memberships.stream()
                .max((left, right) -> {
                    Instant leftAt = left.getActiveAt() == null ? Instant.EPOCH : left.getActiveAt();
                    Instant rightAt = right.getActiveAt() == null ? Instant.EPOCH : right.getActiveAt();
                    return leftAt.compareTo(rightAt);
                })
                .orElse(null);
        List<UserProfileResponse.ProfileAccountResponse> accounts = new ArrayList<>();
        for (AccountUser membership : memberships) {
            accounts.add(new UserProfileResponse.ProfileAccountResponse(
                    membership.getAccount().getId(),
                    membership.getAccount().getName(),
                    membership.getAccount().statusName(),
                    membership.getActiveAt(),
                    membership.roleName(),
                    List.of(membership.roleName()),
                    membership.availabilityName(),
                    membership.availabilityName(),
                    membership.isAutoOffline(),
                    true
            ));
        }
        return new UserProfileResponse(
                "",
                active == null ? null : active.getAccount().getId(),
                user.availableName(),
                "",
                true,
                user.getDisplayName(),
                user.getMessageSignature(),
                user.getEmail(),
                user.getId(),
                null,
                user.getName(),
                user.getProvider(),
                user.getPubsubToken(),
                active == null ? "agent" : active.roleName(),
                user.getUiSettings(),
                user.getUid(),
                user.getType(),
                accounts
        );
    }
}
