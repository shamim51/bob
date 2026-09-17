package com.chatwoot.api.security;

import com.chatwoot.api.domain.AccountUser;
import com.chatwoot.api.domain.User;
import com.chatwoot.api.repo.AccountUserRepository;
import com.chatwoot.api.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CurrentUserService {

    private final UserRepository users;
    private final AccountUserRepository accountUsers;

    public CurrentUserService(UserRepository users, AccountUserRepository accountUsers) {
        this.users = users;
        this.accountUsers = accountUsers;
    }

    public User requireUser() {
        String email = jwtEmail();
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not provisioned"));
        List<AccountUser> memberships = accountUsers.findByUser_Id(user.getId());
        if (!memberships.isEmpty()) {
            user.setCurrentAccountUser(memberships.getFirst());
        }
        return user;
    }

    public AccountUser requireMembership(Integer accountId) {
        User user = requireUser();
        AccountUser membership = accountUsers.findByAccount_IdAndUser_Id(accountId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not a member of this account"));
        user.setCurrentAccountUser(membership);
        membership.getUser().setCurrentAccountUser(membership);
        return membership;
    }

    public List<AccountUser> memberships(User user) {
        return accountUsers.findByUser_Id(user.getId());
    }

    private String jwtEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT missing email");
        }
        return email;
    }
}
