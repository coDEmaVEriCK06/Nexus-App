package com.nexus.chat;

import com.nexus.AbstractIntegrationTest;
import com.nexus.chat.dto.ConversationSummaryResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ReadStateTest extends AbstractIntegrationTest {

    @Autowired private ChatService chat;
    @Autowired private UserRepository users;

    @Test
    void unreadCountReflectsIncomingMessagesAndClearsOnMarkRead() {
        users.save(new User("rs_alice", "h"));
        users.save(new User("rs_bob", "h"));

        chat.sendDirectMessage("rs_bob", new SendDirectMessageRequest("rs_alice", "hi 1"));
        chat.sendDirectMessage("rs_bob", new SendDirectMessageRequest("rs_alice", "hi 2"));

        List<ConversationSummaryResponse> aliceInbox = chat.listConversations("rs_alice");
        assertThat(aliceInbox).hasSize(1);
        assertThat(aliceInbox.get(0).unreadCount()).isEqualTo(2);

        assertThat(chat.listConversations("rs_bob").get(0).unreadCount()).isEqualTo(0);

        Long conversationId = aliceInbox.get(0).conversationId();
        chat.markConversationRead("rs_alice", conversationId);
        assertThat(chat.listConversations("rs_alice").get(0).unreadCount()).isEqualTo(0);
    }
}
