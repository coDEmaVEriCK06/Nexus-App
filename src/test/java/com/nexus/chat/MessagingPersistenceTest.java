package com.nexus.chat;

import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MessagingPersistenceTest {

    @Autowired private UserRepository users;
    @Autowired private ConversationRepository conversations;
    @Autowired private ConversationMemberRepository members;
    @Autowired private MessageRepository messages;

    @Test
    void persistsConversationMembersAndMessage() {
        User alice = users.save(new User("p3a_alice", "h"));
        User bob = users.save(new User("p3a_bob", "h"));

        Conversation conversation = new Conversation(ConversationType.DIRECT, null);
        conversation.setDirectKey(alice.getId() + "_" + bob.getId());
        conversation = conversations.save(conversation);

        members.save(new ConversationMember(conversation, alice, MemberRole.MEMBER));
        members.save(new ConversationMember(conversation, bob, MemberRole.MEMBER));

        Message message = messages.save(new Message(conversation, alice, "hello bob"));

        assertThat(message.getId()).isNotNull();
        assertThat(members.existsByConversationIdAndUserId(conversation.getId(), bob.getId())).isTrue();
        assertThat(members.existsByConversationIdAndUserId(conversation.getId(), 999_999L)).isFalse();
        assertThat(messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId()))
                .hasSize(1)
                .first()
                .extracting(Message::getContent)
                .isEqualTo("hello bob");
        assertThat(conversations.findByDirectKey(alice.getId() + "_" + bob.getId())).isPresent();
    }
}
