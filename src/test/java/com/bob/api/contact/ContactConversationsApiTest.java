package com.bob.api.contact;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BobApplication.class)
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
@Transactional
class ContactConversationsApiTest {

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
    Integer contactId;
    Integer olderDisplayId;
    Integer currentDisplayId;
    Integer newerDisplayId;

    @BeforeEach
    void seed() {
        Account account = new Account();
        account.setName("Contact Conv Account");
        account = accounts.save(account);
        accountId = account.getId();

        User admin = user("Admin", "admin-cc@example.com");
        User agent = user("Agent", "agent-cc@example.com");
        membership(account, admin, AccountUser.ROLE_ADMINISTRATOR);
        membership(account, agent, AccountUser.ROLE_AGENT);

        Inbox inbox1 = inbox(accountId, "One", 21);
        Inbox inbox2 = inbox(accountId, "Two", 22);
        member(inbox1.getId(), agent.getId());

        Contact contact = new Contact();
        contact.setAccountId(accountId);
        contact.setName("Customer");
        contact.setEmail("customer-cc@example.com");
        contact = contacts.save(contact);
        contactId = contact.getId();

        ContactInbox contactInbox1 = contactInbox(contact, inbox1, "src-cc-1");
        ContactInbox contactInbox2 = contactInbox(contact, inbox2, "src-cc-2");

        Instant now = Instant.now();
        olderDisplayId = conversation(accountId, inbox1, contact, contactInbox1, now.minus(3, ChronoUnit.DAYS));
        currentDisplayId = conversation(accountId, inbox1, contact, contactInbox1, now.minus(2, ChronoUnit.DAYS));
        newerDisplayId = conversation(accountId, inbox1, contact, contactInbox1, now.minus(1, ChronoUnit.DAYS));
        this.inbox2 = inbox2;
        this.contactInbox2 = contactInbox2;
        this.contact = contact;
    }

    Inbox inbox2;
    ContactInbox contactInbox2;
    Contact contact;

    @Test
    void historyLatestFirstAndAgentSeesOnlyAssignedInbox() throws Exception {
        Integer otherInboxDisplayId = conversation(
                accountId,
                inbox2,
                contact,
                contactInbox2,
                Instant.now().minus(36, ChronoUnit.HOURS)
        );
        mockMvc.perform(get("/api/v1/accounts/{id}/contacts/{contactId}/conversations", accountId, contactId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "admin-cc@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(4));

        mockMvc.perform(get("/api/v1/accounts/{id}/contacts/{contactId}/conversations", accountId, contactId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent-cc@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(3))
                .andExpect(jsonPath("$.payload[?(@.id==" + otherInboxDisplayId + ")]").doesNotExist())
                .andExpect(jsonPath("$.payload[0].id").value(newerDisplayId));
    }

    @Test
    void neighbourWindowAroundConversation() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/contacts/{contactId}/conversations", accountId, contactId)
                        .param("conversation_id", String.valueOf(currentDisplayId))
                        .with(jwt().jwt(jwt -> jwt.claim("email", "admin-cc@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(3))
                .andExpect(jsonPath("$.payload[0].id").value(olderDisplayId))
                .andExpect(jsonPath("$.payload[1].id").value(currentDisplayId))
                .andExpect(jsonPath("$.payload[2].id").value(newerDisplayId));
    }

    @Test
    void agentNeighbourWindowSkipsInaccessibleInbox() throws Exception {
        conversation(
                accountId,
                inbox2,
                contact,
                contactInbox2,
                Instant.now().minus(36, ChronoUnit.HOURS)
        );
        mockMvc.perform(get("/api/v1/accounts/{id}/contacts/{contactId}/conversations", accountId, contactId)
                        .param("conversation_id", String.valueOf(currentDisplayId))
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent-cc@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(3))
                .andExpect(jsonPath("$.payload[0].id").value(olderDisplayId))
                .andExpect(jsonPath("$.payload[1].id").value(currentDisplayId))
                .andExpect(jsonPath("$.payload[2].id").value(newerDisplayId));
    }

    @Test
    void unknownConversationIdIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/contacts/{contactId}/conversations", accountId, contactId)
                        .param("conversation_id", "999999")
                        .with(jwt().jwt(jwt -> jwt.claim("email", "admin-cc@example.com"))))
                .andExpect(status().isNotFound());
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

    private ContactInbox contactInbox(Contact contact, Inbox inbox, String sourceId) {
        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId(sourceId);
        return contactInboxes.save(contactInbox);
    }

    private Integer conversation(
            Integer accountId,
            Inbox inbox,
            Contact contact,
            ContactInbox contactInbox,
            Instant createdAt
    ) {
        Conversation conversation = new Conversation();
        conversation.setAccountId(accountId);
        conversation.setInbox(inbox);
        conversation.setContact(contact);
        conversation.setContactInbox(contactInbox);
        conversation.setStatus(Conversation.STATUS_OPEN);
        conversation.setDisplayId(displayIds.next(accountId));
        conversation.setCreatedAt(createdAt);
        conversation.setLastActivityAt(createdAt);
        return conversations.save(conversation).getDisplayId();
    }
}
