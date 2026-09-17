package com.chatwoot.api.config;

import com.chatwoot.api.conversation.service.ConversationDisplayIdService;
import com.chatwoot.api.messaging.builder.MessageBuilder;
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
import com.chatwoot.api.account.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "chatwoot.seed.enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private final AccountRepository accounts;
    private final UserRepository users;
    private final AccountUserRepository accountUsers;
    private final InboxRepository inboxes;
    private final InboxMemberRepository inboxMembers;
    private final ContactRepository contacts;
    private final ContactInboxRepository contactInboxes;
    private final ConversationRepository conversations;
    private final ConversationDisplayIdService displayIds;
    private final MessageBuilder messageBuilder;

    public DemoDataLoader(
            AccountRepository accounts,
            UserRepository users,
            AccountUserRepository accountUsers,
            InboxRepository inboxes,
            InboxMemberRepository inboxMembers,
            ContactRepository contacts,
            ContactInboxRepository contactInboxes,
            ConversationRepository conversations,
            ConversationDisplayIdService displayIds,
            MessageBuilder messageBuilder
    ) {
        this.accounts = accounts;
        this.users = users;
        this.accountUsers = accountUsers;
        this.inboxes = inboxes;
        this.inboxMembers = inboxMembers;
        this.contacts = contacts;
        this.contactInboxes = contactInboxes;
        this.conversations = conversations;
        this.displayIds = displayIds;
        this.messageBuilder = messageBuilder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.findByEmailIgnoreCase("agent@example.com").isPresent()) {
            return;
        }
        Account account = new Account();
        account.setName("Acme Support");
        account = accounts.save(account);

        User agent = user("Agent One", "agent@example.com");
        User admin = user("Admin One", "admin@example.com");
        agent = users.save(agent);
        admin = users.save(admin);

        membership(account, agent, AccountUser.ROLE_AGENT);
        membership(account, admin, AccountUser.ROLE_ADMINISTRATOR);

        Inbox inbox = new Inbox();
        inbox.setAccountId(account.getId());
        inbox.setChannelId(1);
        inbox.setChannelType(Inbox.CHANNEL_API);
        inbox.setName("API Inbox");
        inbox = inboxes.save(inbox);

        member(inbox, agent);
        member(inbox, admin);

        Contact contact = new Contact();
        contact.setAccountId(account.getId());
        contact.setName("Jane Customer");
        contact.setEmail("jane@example.com");
        contact = contacts.save(contact);

        ContactInbox contactInbox = new ContactInbox();
        contactInbox.setContact(contact);
        contactInbox.setInbox(inbox);
        contactInbox.setSourceId("api:jane");
        contactInbox = contactInboxes.save(contactInbox);

        Conversation openMine = conversation(account, inbox, contact, contactInbox, agent, Conversation.STATUS_OPEN);
        Conversation unassigned = conversation(account, inbox, contact, contactInbox, null, Conversation.STATUS_OPEN);

        messageBuilder.perform(agent, openMine, new MessageBuilder.CreateMessageParams(
                "Hello, I need help", false, null, "incoming", "text", null, "src-1"));
        messageBuilder.perform(agent, openMine, new MessageBuilder.CreateMessageParams(
                "Hi Jane, we are looking into this.", false, UUID.randomUUID().toString(), "outgoing", "text", null, null));
        messageBuilder.perform(agent, unassigned, new MessageBuilder.CreateMessageParams(
                "Anyone there?", false, null, "incoming", "text", null, "src-2"));
    }

    private User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPubsubToken(UUID.randomUUID().toString());
        return user;
    }

    private void membership(Account account, User user, int role) {
        AccountUser membership = new AccountUser();
        membership.setAccount(account);
        membership.setUser(user);
        membership.setRole(role);
        accountUsers.save(membership);
    }

    private void member(Inbox inbox, User user) {
        InboxMember member = new InboxMember();
        member.setInboxId(inbox.getId());
        member.setUserId(user.getId());
        inboxMembers.save(member);
    }

    private Conversation conversation(
            Account account,
            Inbox inbox,
            Contact contact,
            ContactInbox contactInbox,
            User assignee,
            int status
    ) {
        Conversation conversation = new Conversation();
        conversation.setAccountId(account.getId());
        conversation.setInbox(inbox);
        conversation.setContact(contact);
        conversation.setContactInbox(contactInbox);
        conversation.setAssignee(assignee);
        conversation.setStatus(status);
        conversation.setDisplayId(displayIds.next(account.getId()));
        conversation.setLastActivityAt(Instant.now());
        return conversations.save(conversation);
    }
}
