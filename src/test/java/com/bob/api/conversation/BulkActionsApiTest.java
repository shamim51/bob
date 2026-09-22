package com.bob.api.conversation;

import com.bob.api.BobApplication;
import com.bob.api.account.model.Account;
import com.bob.api.account.model.AccountUser;
import com.bob.api.account.model.User;
import com.bob.api.account.repository.AccountRepository;
import com.bob.api.account.repository.AccountUserRepository;
import com.bob.api.account.repository.UserRepository;
import com.bob.api.config.TestJwtConfig;
import com.bob.api.contact.model.Contact;
import com.bob.api.contact.model.ContactInbox;
import com.bob.api.contact.repository.ContactInboxRepository;
import com.bob.api.contact.repository.ContactRepository;
import com.bob.api.conversation.model.Conversation;
import com.bob.api.conversation.repository.ConversationRepository;
import com.bob.api.conversation.service.ConversationDisplayIdService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BobApplication.class)
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
@Transactional
class BulkActionsApiTest {

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
    @Autowired
    ContactRepository contacts;
    @Autowired
    ContactInboxRepository contactInboxes;
    @Autowired
    ConversationRepository conversations;
    @Autowired
    ConversationDisplayIdService displayIds;

    Integer accountId;
    User agent;
    User assignee;
    Inbox inbox;
    Contact contact;
    Integer targetDisplayId;
    Integer otherDisplayId;

    @BeforeEach
    void seed() {
        Account account = new Account();
        account.setName("Bulk Account");
        account = accounts.save(account);
        accountId = account.getId();

        agent = saveUser("agent-bulk@example.com", "Agent Bulk");
        assignee = saveUser("assignee-bulk@example.com", "Assignee Bulk");
        membership(account, agent, AccountUser.ROLE_AGENT);
        membership(account, assignee, AccountUser.ROLE_AGENT);

        inbox = saveInbox("API");
        inboxMembers.save(member(inbox.getId(), agent.getId()));
        inboxMembers.save(member(inbox.getId(), assignee.getId()));

        contact = saveContact("bulk-customer@example.com");
        ContactInbox contactInbox = saveContactInbox(contact, inbox, "src-bulk");

        Conversation target = saveConversation(inbox, contact, contactInbox, null);
        targetDisplayId = target.getDisplayId();
        Conversation other = saveConversation(inbox, contact, contactInbox, null);
        otherDisplayId = other.getDisplayId();
    }

    @Test
    void assignsAgentOnSelectedDisplayIdsAndLeavesOthers() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{id}/bulk_actions", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", agent.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"Conversation","ids":[%d],"fields":{"assignee_id":%d}}
                                """.formatted(targetDisplayId, assignee.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        Conversation updated = conversations.findByAccountIdAndDisplayId(accountId, targetDisplayId).orElseThrow();
        Conversation unchanged = conversations.findByAccountIdAndDisplayId(accountId, otherDisplayId).orElseThrow();
        assertThat(updated.getAssignee().getId()).isEqualTo(assignee.getId());
        assertThat(unchanged.getAssignee()).isNull();
    }

    @Test
    void clearsAssigneeWhenAssigneeIdIsNull() throws Exception {
        Conversation existing = conversations.findByAccountIdAndDisplayId(accountId, targetDisplayId).orElseThrow();
        existing.setAssignee(assignee);
        conversations.save(existing);

        mockMvc.perform(post("/api/v1/accounts/{id}/bulk_actions", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", agent.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"Conversation","ids":[%d],"fields":{"assignee_id":null}}
                                """.formatted(targetDisplayId)))
                .andExpect(status().isOk());

        Conversation updated = conversations.findByAccountIdAndDisplayId(accountId, targetDisplayId).orElseThrow();
        assertThat(updated.getAssignee()).isNull();
    }

    @Test
    void agentWithoutInboxMembershipDoesNotUpdateConversation() throws Exception {
        User outsider = saveUser("outsider-bulk@example.com", "Outsider");
        membership(accounts.findById(accountId).orElseThrow(), outsider, AccountUser.ROLE_AGENT);

        mockMvc.perform(post("/api/v1/accounts/{id}/bulk_actions", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", outsider.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"Conversation","ids":[%d],"fields":{"assignee_id":%d}}
                                """.formatted(targetDisplayId, assignee.getId())))
                .andExpect(status().isOk());

        Conversation updated = conversations.findByAccountIdAndDisplayId(accountId, targetDisplayId).orElseThrow();
        assertThat(updated.getAssignee()).isNull();
    }

    @Test
    void unknownTypeReturnsUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{id}/bulk_actions", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", agent.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"Test","ids":[%d],"fields":{"status":"snoozed"}}
                                """.formatted(targetDisplayId)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false));

        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, targetDisplayId).orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(Conversation.STATUS_OPEN);
    }

    @Test
    void nullStatusDoesNotChangeStatus() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{id}/bulk_actions", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", agent.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"Conversation","ids":[%d],"fields":{"status":null}}
                                """.formatted(targetDisplayId)))
                .andExpect(status().isOk());

        Conversation conversation = conversations.findByAccountIdAndDisplayId(accountId, targetDisplayId).orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(Conversation.STATUS_OPEN);
    }

    private User saveUser(String email, String name) {
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

    private Inbox saveInbox(String name) {
        Inbox created = new Inbox();
        created.setAccountId(accountId);
        created.setChannelId(1);
        created.setChannelType(Inbox.CHANNEL_API);
        created.setName(name);
        return inboxes.save(created);
    }

    private InboxMember member(Integer inboxId, Integer userId) {
        InboxMember member = new InboxMember();
        member.setInboxId(inboxId);
        member.setUserId(userId);
        return member;
    }

    private Contact saveContact(String email) {
        Contact created = new Contact();
        created.setAccountId(accountId);
        created.setName("Bulk Customer");
        created.setEmail(email);
        return contacts.save(created);
    }

    private ContactInbox saveContactInbox(Contact contact, Inbox inbox, String sourceId) {
        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId(sourceId);
        return contactInboxes.save(contactInbox);
    }

    private Conversation saveConversation(Inbox inbox, Contact contact, ContactInbox contactInbox, User assignee) {
        Conversation conversation = new Conversation();
        conversation.setAccountId(accountId);
        conversation.setInbox(inbox);
        conversation.setContact(contact);
        conversation.setContactInbox(contactInbox);
        conversation.setAssignee(assignee);
        conversation.setStatus(Conversation.STATUS_OPEN);
        conversation.setDisplayId(displayIds.next(accountId));
        conversation.setLastActivityAt(Instant.now());
        return conversations.save(conversation);
    }
}
