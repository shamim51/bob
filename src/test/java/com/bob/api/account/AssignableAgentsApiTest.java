package com.bob.api.account;

import com.bob.api.BobApplication;
import com.bob.api.account.model.Account;
import com.bob.api.account.model.AccountUser;
import com.bob.api.account.model.User;
import com.bob.api.account.repository.AccountRepository;
import com.bob.api.account.repository.AccountUserRepository;
import com.bob.api.account.repository.UserRepository;
import com.bob.api.config.TestJwtConfig;
import com.bob.api.inbox.model.Inbox;
import com.bob.api.inbox.model.InboxMember;
import com.bob.api.inbox.repository.InboxMemberRepository;
import com.bob.api.inbox.repository.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BobApplication.class)
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
@Transactional
class AssignableAgentsApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    AccountRepository accounts;
    @Autowired
    UserRepository users;
    @Autowired
    AccountUserRepository accountUsers;
    @Autowired
    InboxRepository inboxes;
    @Autowired
    InboxMemberRepository inboxMembers;

    Integer accountId;
    Integer inbox1Id;
    Integer inbox2Id;

    @BeforeEach
    void seed() {
        Account account = new Account();
        account.setName("Assignable Account");
        account = accounts.save(account);
        accountId = account.getId();

        User admin = user("Admin", "admin-assign@example.com");
        User agentBoth = user("Both", "agent-both@example.com");
        User agentOne = user("One", "agent-one@example.com");
        User outsider = user("Out", "agent-out@example.com");

        membership(account, admin, AccountUser.ROLE_ADMINISTRATOR);
        membership(account, agentBoth, AccountUser.ROLE_AGENT);
        membership(account, agentOne, AccountUser.ROLE_AGENT);
        membership(account, outsider, AccountUser.ROLE_AGENT);

        Inbox inbox1 = inbox(accountId, "Inbox 1", 11);
        Inbox inbox2 = inbox(accountId, "Inbox 2", 12);
        inbox1Id = inbox1.getId();
        inbox2Id = inbox2.getId();

        member(inbox1Id, agentBoth.getId());
        member(inbox2Id, agentBoth.getId());
        member(inbox1Id, agentOne.getId());
    }

    @Test
    void intersectionPlusAdministrators() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/assignable_agents", accountId)
                        .param("inbox_ids[]", String.valueOf(inbox1Id), String.valueOf(inbox2Id))
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent-both@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(2))
                .andExpect(jsonPath("$.payload[?(@.email=='agent-both@example.com')]").exists())
                .andExpect(jsonPath("$.payload[?(@.email=='admin-assign@example.com')]").exists())
                .andExpect(jsonPath("$.payload[?(@.email=='agent-one@example.com')]").doesNotExist())
                .andExpect(jsonPath("$.payload[0].assignee_type").doesNotExist());
    }

    @Test
    void includeAiAssigneesAddsUserAssigneeType() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/assignable_agents", accountId)
                        .param("inbox_ids[]", String.valueOf(inbox1Id))
                        .param("include_ai_assignees", "true")
                        .with(jwt().jwt(jwt -> jwt.claim("email", "admin-assign@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(3))
                .andExpect(jsonPath("$.payload[0].assignee_type").value("User"));
    }

    @Test
    void unknownInboxIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/assignable_agents", accountId)
                        .param("inbox_ids[]", "999999")
                        .with(jwt().jwt(jwt -> jwt.claim("email", "admin-assign@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingInboxIdsIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/assignable_agents", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "admin-assign@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentOutsideInboxIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/assignable_agents", accountId)
                        .param("inbox_ids[]", String.valueOf(inbox1Id))
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent-out@example.com"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agentsIndexOmitsAssigneeType() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/agents", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "admin-assign@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignee_type").doesNotExist());
    }

    private User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPubsubToken("token-" + email);
        return users.save(user);
    }

    private void membership(Account account, User user, int role) {
        AccountUser membership = new AccountUser();
        membership.setAccount(account);
        membership.setUser(user);
        membership.setRole(role);
        accountUsers.save(membership);
    }

    private Inbox inbox(Integer accountId, String name, int channelId) {
        Inbox inbox = new Inbox();
        inbox.setAccountId(accountId);
        inbox.setChannelId(channelId);
        inbox.setChannelType(Inbox.CHANNEL_API);
        inbox.setName(name);
        return inboxes.save(inbox);
    }

    private void member(Integer inboxId, Integer userId) {
        InboxMember member = new InboxMember();
        member.setInboxId(inboxId);
        member.setUserId(userId);
        inboxMembers.save(member);
    }
}
