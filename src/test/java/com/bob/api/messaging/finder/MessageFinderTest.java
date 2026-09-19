package com.bob.api.messaging.finder;

import com.bob.api.BobApplication;
import com.bob.api.config.TestJwtConfig;
import com.bob.api.account.model.Account;
import com.bob.api.conversation.model.Conversation;
import com.bob.api.inbox.model.Inbox;
import com.bob.api.messaging.model.Message;
import com.bob.api.account.repository.AccountRepository;
import com.bob.api.conversation.repository.ConversationRepository;
import com.bob.api.inbox.repository.InboxRepository;
import com.bob.api.messaging.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BobApplication.class)
@Import(TestJwtConfig.class)
@Transactional
class MessageFinderTest {

    @Autowired
    MessageFinder messageFinder;
    @Autowired
    AccountRepository accounts;
    @Autowired
    InboxRepository inboxes;
    @Autowired
    ConversationRepository conversations;
    @Autowired
    MessageRepository messages;

    @Test
    void latestIsTwentyOldestOfTheLastPageAscending() {
        Account account = new Account();
        account.setName("A");
        account = accounts.save(account);
        Inbox inbox = new Inbox();
        inbox.setAccountId(account.getId());
        inbox.setChannelId(1);
        inbox.setName("I");
        inbox = inboxes.save(inbox);
        Conversation conversation = new Conversation();
        conversation.setAccountId(account.getId());
        conversation.setInbox(inbox);
        conversation.setDisplayId(1);
        conversation.setLastActivityAt(Instant.now());
        conversation = conversations.save(conversation);

        for (int i = 0; i < 25; i++) {
            Message message = new Message();
            message.setAccountId(account.getId());
            message.setInboxId(inbox.getId());
            message.setConversation(conversation);
            message.setMessageType(Message.TYPE_INCOMING);
            message.setContent("m-" + i);
            message.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i));
            messages.save(message);
        }

        List<Message> latest = messageFinder.perform(conversation, null, null);
        assertThat(latest).hasSize(20);
        assertThat(latest.getFirst().getContent()).isEqualTo("m-5");
        assertThat(latest.getLast().getContent()).isEqualTo("m-24");

        List<Message> before = messageFinder.perform(conversation, null, latest.getFirst().getId());
        assertThat(before).hasSize(5);
        assertThat(before.getFirst().getContent()).isEqualTo("m-0");

        List<Message> after = messageFinder.perform(conversation, latest.getFirst().getId(), null);
        assertThat(after).extracting(Message::getContent).contains("m-6");
        assertThat(after).allMatch(message -> message.getId() > latest.getFirst().getId());
    }
}
