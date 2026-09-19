package com.chatwoot.api.integration.facebook;

import com.chatwoot.api.ChatwootApiApplication;
import com.chatwoot.api.account.model.Account;
import com.chatwoot.api.account.model.AccountUser;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.account.repository.AccountRepository;
import com.chatwoot.api.account.repository.AccountUserRepository;
import com.chatwoot.api.account.repository.UserRepository;
import com.chatwoot.api.config.TestJwtConfig;
import com.chatwoot.api.contact.model.Contact;
import com.chatwoot.api.contact.model.ContactInbox;
import com.chatwoot.api.contact.repository.ContactInboxRepository;
import com.chatwoot.api.contact.repository.ContactRepository;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.conversation.repository.ConversationRepository;
import com.chatwoot.api.conversation.service.ConversationDisplayIdService;
import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.inbox.model.InboxMember;
import com.chatwoot.api.inbox.repository.InboxMemberRepository;
import com.chatwoot.api.inbox.repository.InboxRepository;
import com.chatwoot.api.integration.facebook.model.FacebookPage;
import com.chatwoot.api.integration.facebook.repository.FacebookPageRepository;
import com.chatwoot.api.integration.facebook.service.FacebookGraphClient;
import com.chatwoot.api.messaging.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ChatwootApiApplication.class)
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
@Transactional
class FacebookChannelApiTest {

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
    FacebookPageRepository facebookPages;
    @Autowired
    ContactRepository contacts;
    @Autowired
    ContactInboxRepository contactInboxes;
    @Autowired
    ConversationRepository conversations;
    @Autowired
    ConversationDisplayIdService displayIds;
    @Autowired
    MessageRepository messages;

    @MockitoBean
    FacebookGraphClient graph;

    Integer accountId;
    Integer agentId;
    Integer secondAgentId;

    @BeforeEach
    void seed() {
        Account account = new Account();
        account.setName("FB Account");
        account = accounts.save(account);
        accountId = account.getId();

        User agent = new User();
        agent.setName("Agent");
        agent.setEmail("fb-agent@example.com");
        agent.setPubsubToken("token-fb-agent");
        agent = users.save(agent);
        agentId = agent.getId();

        User second = new User();
        second.setName("Second");
        second.setEmail("fb-second@example.com");
        second.setPubsubToken("token-fb-second");
        second = users.save(second);
        secondAgentId = second.getId();

        AccountUser membership = new AccountUser();
        membership.setAccount(account);
        membership.setUser(agent);
        membership.setRole(AccountUser.ROLE_ADMINISTRATOR);
        accountUsers.save(membership);

        AccountUser secondMembership = new AccountUser();
        secondMembership.setAccount(account);
        secondMembership.setUser(second);
        secondMembership.setRole(AccountUser.ROLE_AGENT);
        accountUsers.save(secondMembership);

        when(graph.exchangeLongLivedToken(any())).thenReturn("long-lived-token");
        when(graph.listPages("long-lived-token")).thenReturn(List.of(
                new FacebookGraphClient.FacebookAccountPage("page-1", "Shop", "page-token-1")
        ));
        when(graph.fetchPageDetails(any())).thenReturn(new FacebookGraphClient.FacebookPageDetails("Shop", null));
        doNothing().when(graph).subscribePage(any(), any());
        when(graph.fetchUserProfile(any(), any())).thenReturn(new FacebookGraphClient.FacebookUserProfile("Jane", "Roe"));
        when(graph.sendText(any(), any(), any())).thenReturn(new FacebookGraphClient.FacebookSendResult("mid-out", null, null));
    }

    @Test
    void facebookPagesMarksExistingAndKeepsJsonShape() throws Exception {
        FacebookPage existing = new FacebookPage();
        existing.setAccountId(accountId);
        existing.setPageId("page-1");
        existing.setUserAccessToken("u");
        existing.setPageAccessToken("p");
        facebookPages.save(existing);

        mockMvc.perform(post("/api/v1/accounts/{id}/callbacks/facebook_pages.json", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "fb-agent@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"omniauth_token\":\"short-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page_details[0].id").value("page-1"))
                .andExpect(jsonPath("$.data.page_details[0].name").value("Shop"))
                .andExpect(jsonPath("$.data.page_details[0].access_token").value("page-token-1"))
                .andExpect(jsonPath("$.data.page_details[0].exists").value(true))
                .andExpect(jsonPath("$.data.user_access_token").value("long-lived-token"));
    }

    @Test
    void registerFacebookPageCreatesInboxAndListIncludesPageId() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{id}/callbacks/register_facebook_page", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "fb-agent@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "user_access_token":"user-token",
                                  "page_access_token":"page-token-1",
                                  "page_id":"page-1",
                                  "inbox_name":"Facebook Shop"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Facebook Shop"))
                .andExpect(jsonPath("$.channel_type").value("Channel::FacebookPage"))
                .andExpect(jsonPath("$.page_id").value("page-1"))
                .andExpect(jsonPath("$.enable_auto_assignment").value(true));

        mockMvc.perform(get("/api/v1/accounts/{id}/inboxes", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "fb-agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].page_id").value("page-1"))
                .andExpect(jsonPath("$.payload[0].provider_name").value("Shop"))
                .andExpect(jsonPath("$.payload[0].reauthorization_required").value(false));

        verify(graph).subscribePage("page-1", "page-token-1");
    }

    @Test
    void reauthorizeReturnsUnprocessableWhenPageMissingFromFacebook() throws Exception {
        FacebookPage page = saveFacebookInbox("page-missing");
        Inbox inbox = inboxes.findByChannelTypeAndChannelId(Inbox.CHANNEL_FACEBOOK, page.getId()).orElseThrow();

        mockMvc.perform(post("/api/v1/accounts/{id}/callbacks/reauthorize_page", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "fb-agent@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"omniauth_token":"short-token","inbox_id":%d}
                                """.formatted(inbox.getId())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void inboxMembersPatchReplacesMembership() throws Exception {
        FacebookPage page = saveFacebookInbox("page-members");
        Inbox inbox = inboxes.findByChannelTypeAndChannelId(Inbox.CHANNEL_FACEBOOK, page.getId()).orElseThrow();

        mockMvc.perform(patch("/api/v1/accounts/{id}/inbox_members", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "fb-agent@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inbox_id":%d,"user_ids":[%d,%d]}
                                """.formatted(inbox.getId(), agentId, secondAgentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(2));

        assertThat(inboxMembers.findByInboxId(inbox.getId())).hasSize(2);
    }

    @Test
    void apiInboxOmitsFacebookKeys() throws Exception {
        Inbox inbox = new Inbox();
        inbox.setAccountId(accountId);
        inbox.setChannelId(99);
        inbox.setChannelType(Inbox.CHANNEL_API);
        inbox.setName("API");
        inboxes.save(inbox);

        mockMvc.perform(get("/api/v1/accounts/{id}/inboxes", accountId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", "fb-agent@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].channel_type").value("Channel::Api"))
                .andExpect(jsonPath("$.payload[0].page_id").doesNotExist());
    }

    @Test
    void webhookVerifyReturnsChallenge() throws Exception {
        mockMvc.perform(get("/bot")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "test-verify")
                        .param("hub.challenge", "challenge-token"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo("challenge-token"));
    }

    @Test
    void webhookRejectsBadSignature() throws Exception {
        mockMvc.perform(post("/bot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=deadbeef")
                        .content("{\"object\":\"page\",\"entry\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhookIncomingCreatesConversationAndMessage() throws Exception {
        saveFacebookInbox("page-1");
        String body = """
                {"object":"page","entry":[{"messaging":[{"sender":{"id":"psid-1"},"recipient":{"id":"page-1"},"timestamp":1,"message":{"mid":"mid-in","text":"hello from fb"}}]}]}
                """;
        mockMvc.perform(post("/bot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(body))
                        .content(body))
                .andExpect(status().isOk());

        assertThat(messages.findAll()).anyMatch(message -> "hello from fb".equals(message.getContent()));
        assertThat(contactInboxes.findAll()).anyMatch(row -> "psid-1".equals(row.getSourceId()));
    }

    @Test
    void webhookSkipsEchoFromThisApp() throws Exception {
        saveFacebookInbox("page-1");
        String body = """
                {"object":"page","entry":[{"messaging":[{"sender":{"id":"page-1"},"recipient":{"id":"psid-1"},"message":{"mid":"mid-echo","text":"echo","is_echo":true,"app_id":"test-app-id"}}]}]}
                """;
        mockMvc.perform(post("/bot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(body))
                        .content(body))
                .andExpect(status().isOk());

        assertThat(messages.findAll()).isEmpty();
    }

    @Test
    void outgoingReplySendsToGraphAndStoresSourceId() throws Exception {
        FacebookPage page = saveFacebookInbox("page-1");
        Inbox inbox = inboxes.findByChannelTypeAndChannelId(Inbox.CHANNEL_FACEBOOK, page.getId()).orElseThrow();

        Contact contact = new Contact();
        contact.setAccountId(accountId);
        contact.setName("Customer");
        contact = contacts.save(contact);

        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId("psid-1");
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

        InboxMember member = new InboxMember();
        member.setInboxId(inbox.getId());
        member.setUserId(agentId);
        inboxMembers.save(member);

        mockMvc.perform(post("/api/v1/accounts/{id}/conversations/{displayId}/messages", accountId, conversation.getDisplayId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", "fb-agent@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"thanks\",\"private\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source_id").value("mid-out"))
                .andExpect(jsonPath("$.status").value("sent"));

        verify(graph).sendText(eq("page-token"), eq("psid-1"), eq("thanks"));
    }

    private FacebookPage saveFacebookInbox(String pageId) {
        FacebookPage page = new FacebookPage();
        page.setAccountId(accountId);
        page.setPageId(pageId);
        page.setUserAccessToken("user-token");
        page.setPageAccessToken("page-token");
        page.setProviderName("Shop");
        page = facebookPages.save(page);

        Inbox inbox = new Inbox();
        inbox.setAccountId(accountId);
        inbox.setName("Facebook");
        inbox.setChannelType(Inbox.CHANNEL_FACEBOOK);
        inbox.setChannelId(page.getId());
        inboxes.save(inbox);
        return page;
    }

    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
