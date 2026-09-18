package com.chatwoot.api.conversation;

import com.chatwoot.api.ChatwootApiApplication;
import com.chatwoot.api.conversation.service.ConversationDisplayIdService;
import com.chatwoot.api.messaging.builder.MessageBuilder;
import com.chatwoot.api.config.TestJwtConfig;
import com.chatwoot.api.account.model.Account;
import com.chatwoot.api.account.model.AccountUser;
import com.chatwoot.api.contact.model.Contact;
import com.chatwoot.api.contact.model.ContactInbox;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.inbox.model.InboxMember;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.account.repository.AccountRepository;
import com.chatwoot.api.account.repository.AccountUserRepository;
import com.chatwoot.api.contact.repository.ContactInboxRepository;
import com.chatwoot.api.contact.repository.ContactRepository;
import com.chatwoot.api.conversation.repository.ConversationRepository;
import com.chatwoot.api.inbox.repository.InboxMemberRepository;
import com.chatwoot.api.inbox.repository.InboxRepository;
import com.chatwoot.api.messaging.repository.MessageRepository;
import com.chatwoot.api.account.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ChatwootApiApplication.class)
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
@Transactional
class ConversationApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
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
    @Autowired
    MessageRepository messages;

    Integer accountId;
    Integer displayId;

    @BeforeEach
    void seed() {
        Account account = new Account();
        account.setName("Test Account");
        account = accounts.save(account);
        accountId = account.getId();

        User agent = new User();
        agent.setName("Agent");
        agent.setEmail("agent@example.com");
        agent.setPubsubToken("token-agent");
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
        contact.setEmail("customer@example.com");
        contact = contacts.save(contact);

        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId("src-1");
        contactInbox = contactInboxes.save(contactInbox);

        Conversation conversation = new Conversation();
        conversation.setAccountId(accountId);
        conversation.setInbox(inbox);
        conversation.setContact(contact);
        conversation.setContactInbox(contactInbox);
        conversation.setAssignee(agent);
        conversation.setStatus(Conversation.STATUS_OPEN);
        conversation.setDisplayId(displayIds.next(accountId));
        conversation.setLastActivityAt(Instant.now());
        conversation = conversations.save(conversation);
        displayId = conversation.getDisplayId();

        messageBuilder.perform(agent, conversation, new MessageBuilder.CreateMessageParams(
                "incoming hello", false, null, "incoming", "text", null, "mid-1"));
        messageBuilder.perform(agent, conversation, new MessageBuilder.CreateMessageParams(
                "outgoing hello", false, "echo-seed", "outgoing", "text", null, null));
    }

    @Test
    void listUsesDisplayIdNotPrimaryKey() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/accounts/{id}/conversations", accountId)
                        .param("assignee_type", "me")
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meta.mine_count").value(1))
                .andExpect(jsonPath("$.data.payload[0].id").value(displayId))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.at("/data/payload/0/id").asInt()).isEqualTo(displayId);
        assertThat(root.at("/data/payload/0/messages/0/conversation_id").asInt()).isEqualTo(displayId);
        assertThat(root.at("/data/payload/0/messages/0/message_type").isInt()).isTrue();
        assertThat(root.at("/data/payload/0/messages/0/status").asText()).isEqualTo("sent");
        assertThat(root.at("/data/payload/0/created_at").isIntegralNumber()).isTrue();
        assertThat(root.at("/data/payload/0/can_reply").asBoolean()).isTrue();
    }

    @Test
    void showUsesTypedConversationPayload() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/conversations/{displayId}", accountId, displayId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(displayId))
                .andExpect(jsonPath("$.meta.channel").value("Channel::Api"))
                .andExpect(jsonPath("$.meta.sender.type").value("contact"))
                .andExpect(jsonPath("$.contact_info_request.available").value(false));
    }

    @Test
    void messageFinderLatestAndBeforeAfter() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}/conversations/{displayId}/messages", accountId, displayId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(2));

        int firstId = messages.findLatestFirst(conversations.findByAccountIdAndDisplayId(accountId, displayId).orElseThrow().getId())
                .getLast()
                .getId();
        mockMvc.perform(get("/api/v1/accounts/{id}/conversations/{displayId}/messages", accountId, displayId)
                        .param("before", String.valueOf(firstId + 10_000))
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void createReturnsEchoIdAndUpdatesLastActivity() throws Exception {
        Conversation before = conversations.findByAccountIdAndDisplayId(accountId, displayId).orElseThrow();
        Instant previous = before.getLastActivityAt();
        String echo = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/accounts/{id}/conversations/{displayId}/messages", accountId, displayId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"thanks","private":false,"echo_id":"%s"}
                                """.formatted(echo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echo_id").value(echo))
                .andExpect(jsonPath("$.conversation_id").value(displayId))
                .andExpect(jsonPath("$.message_type").value(1))
                .andExpect(jsonPath("$.status").value("sent"));
        Conversation after = conversations.findByAccountIdAndDisplayId(accountId, displayId).orElseThrow();
        assertThat(after.getLastActivityAt()).isAfterOrEqualTo(previous);
    }

    @Test
    void profileAndSupportingReads() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.data.email").value("agent@example.com"))
                .andExpect(jsonPath("$.payload.data.accounts[0].id").value(accountId))
                .andExpect(jsonPath("$.payload.data.accounts[0].permissions[0]").value("agent"));
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId));
        mockMvc.perform(get("/api/v1/accounts/{id}/inboxes", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].channel_type").value("Channel::Api"));
        mockMvc.perform(get("/api/v1/accounts/{id}/agents", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("agent@example.com"));
        mockMvc.perform(get("/api/v1/accounts/{id}/labels", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray());
    }
}
