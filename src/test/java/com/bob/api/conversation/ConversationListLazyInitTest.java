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
import com.bob.api.messaging.builder.MessageBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BobApplication.class)
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
class ConversationListLazyInitTest {

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
    @Autowired
    MessageBuilder messageBuilder;

    @Test
    void listUnassignedDoesNotTouchLazyConversationProxy() throws Exception {
        String email = "agent-lazy-" + UUID.randomUUID() + "@example.com";

        Account account = new Account();
        account.setName("Lazy Account");
        account = accounts.save(account);
        Integer accountId = account.getId();

        User agent = new User();
        agent.setName("Agent");
        agent.setEmail(email);
        agent.setPubsubToken("token-" + UUID.randomUUID());
        agent = users.save(agent);

        AccountUser membership = new AccountUser();
        membership.setAccount(account);
        membership.setUser(agent);
        membership.setRole(AccountUser.ROLE_AGENT);
        accountUsers.save(membership);

        Inbox inbox = new Inbox();
        inbox.setAccountId(accountId);
        inbox.setChannelId(1);
        inbox.setChannelType(Inbox.CHANNEL_API);
        inbox.setName("API");
        inbox = inboxes.save(inbox);

        InboxMember member = new InboxMember();
        member.setInboxId(inbox.getId());
        member.setUserId(agent.getId());
        inboxMembers.save(member);

        Contact contact = new Contact();
        contact.setAccountId(accountId);
        contact.setName("Customer");
        contact.setEmail("customer-lazy-" + UUID.randomUUID() + "@example.com");
        contact = contacts.save(contact);

        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId("src-lazy-" + UUID.randomUUID());
        contactInbox = contactInboxes.save(contactInbox);

        Conversation conversation = new Conversation();
        conversation.setAccountId(accountId);
        conversation.setInbox(inbox);
        conversation.setContact(contact);
        conversation.setContactInbox(contactInbox);
        conversation.setStatus(Conversation.STATUS_OPEN);
        conversation.setDisplayId(displayIds.next(accountId));
        conversation.setLastActivityAt(Instant.now());
        conversation = conversations.save(conversation);
        Integer displayId = conversation.getDisplayId();

        messageBuilder.perform(agent, conversation, new MessageBuilder.CreateMessageParams(
                "incoming hello", false, null, "incoming", "text", null, "mid-lazy"));

        mockMvc.perform(get("/api/v1/accounts/{id}/conversations", accountId)
                        .param("status", "open")
                        .param("assignee_type", "unassigned")
                        .param("page", "1")
                        .param("sort_by", "last_activity_at_desc")
                        .with(jwt().jwt(jwt -> jwt.claim("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payload[0].id").value(displayId))
                .andExpect(jsonPath("$.data.payload[0].messages[0].conversation_id").value(displayId))
                .andExpect(jsonPath("$.data.payload[0].messages[0].sender.type").value("contact"));
    }

    @Test
    void contactConversationListDoesNotTouchLazyConversationProxy() throws Exception {
        String email = "agent-cc-lazy-" + UUID.randomUUID() + "@example.com";

        Account account = new Account();
        account.setName("Lazy Contact Account");
        account = accounts.save(account);
        Integer accountId = account.getId();

        User agent = new User();
        agent.setName("Agent");
        agent.setEmail(email);
        agent.setPubsubToken("token-" + UUID.randomUUID());
        agent = users.save(agent);

        AccountUser membership = new AccountUser();
        membership.setAccount(account);
        membership.setUser(agent);
        membership.setRole(AccountUser.ROLE_AGENT);
        accountUsers.save(membership);

        Inbox inbox = new Inbox();
        inbox.setAccountId(accountId);
        inbox.setChannelId(1);
        inbox.setChannelType(Inbox.CHANNEL_API);
        inbox.setName("API");
        inbox = inboxes.save(inbox);

        InboxMember member = new InboxMember();
        member.setInboxId(inbox.getId());
        member.setUserId(agent.getId());
        inboxMembers.save(member);

        Contact contact = new Contact();
        contact.setAccountId(accountId);
        contact.setName("Customer");
        contact.setEmail("customer-cc-lazy-" + UUID.randomUUID() + "@example.com");
        contact = contacts.save(contact);

        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId("src-cc-lazy-" + UUID.randomUUID());
        contactInbox = contactInboxes.save(contactInbox);

        Conversation conversation = new Conversation();
        conversation.setAccountId(accountId);
        conversation.setInbox(inbox);
        conversation.setContact(contact);
        conversation.setContactInbox(contactInbox);
        conversation.setStatus(Conversation.STATUS_OPEN);
        conversation.setDisplayId(displayIds.next(accountId));
        conversation.setLastActivityAt(Instant.now());
        conversation = conversations.save(conversation);
        Integer displayId = conversation.getDisplayId();

        messageBuilder.perform(agent, conversation, new MessageBuilder.CreateMessageParams(
                "incoming hello", false, null, "incoming", "text", null, "mid-cc-lazy"));

        mockMvc.perform(get("/api/v1/accounts/{id}/contacts/{contactId}/conversations", accountId, contact.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].id").value(displayId))
                .andExpect(jsonPath("$.payload[0].messages[0].conversation_id").value(displayId))
                .andExpect(jsonPath("$.payload[0].meta.sender.type").value("contact"));
    }
}
