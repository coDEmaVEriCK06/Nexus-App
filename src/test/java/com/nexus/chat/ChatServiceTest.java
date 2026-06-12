package com.nexus.chat;

import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.common.ForbiddenAccessException;
import com.nexus.common.PageResponse;
import com.nexus.common.ResourceNotFoundException;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ChatServiceTest {

    @Autowired private ChatService chat;
    @Autowired private UserRepository users;

    @Test
    void directMessageFlowWithAuthorization() {
        users.save(new User("p3_alice", "h"));
        users.save(new User("p3_bob", "h"));
        users.save(new User("p3_carol", "h"));

        MessageResponse sent = chat.sendDirectMessage("p3_alice",
                new SendDirectMessageRequest("p3_bob", "hi bob"));
        assertThat(sent.id()).isNotNull();
        assertThat(sent.senderUsername()).isEqualTo("p3_alice");

        Long conversationId = sent.conversationId();

        PageResponse<MessageResponse> bobView =
                chat.getConversationMessages("p3_bob", conversationId, 0, 20);
        assertThat(bobView.content()).hasSize(1);
        assertThat(bobView.content().get(0).content()).isEqualTo("hi bob");

        assertThatThrownBy(() -> chat.getConversationMessages("p3_carol", conversationId, 0, 20))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void reusesTheSameDirectConversation() {
        users.save(new User("p3_x", "h"));
        users.save(new User("p3_y", "h"));

        MessageResponse first = chat.sendDirectMessage("p3_x",
                new SendDirectMessageRequest("p3_y", "one"));
        MessageResponse second = chat.sendDirectMessage("p3_y",
                new SendDirectMessageRequest("p3_x", "two"));

        assertThat(second.conversationId()).isEqualTo(first.conversationId());
    }

    @Test
    void rejectsMessageToUnknownRecipient() {
        users.save(new User("p3_solo", "h"));
        assertThatThrownBy(() -> chat.sendDirectMessage("p3_solo",
                new SendDirectMessageRequest("ghost", "anyone there?")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
