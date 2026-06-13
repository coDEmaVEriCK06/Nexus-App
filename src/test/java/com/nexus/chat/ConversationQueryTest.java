package com.nexus.chat;

import com.nexus.AbstractIntegrationTest;
import com.nexus.chat.dto.ConversationSummaryResponse;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.GroupMemberResponse;
import com.nexus.chat.dto.GroupResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.common.ForbiddenAccessException;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ConversationQueryTest extends AbstractIntegrationTest {

    @Autowired private ChatService chat;
    @Autowired private GroupService groups;
    @Autowired private UserRepository users;

    @Test
    void listConversationsReturnsDirectConversationWithPeerAndLastMessage() {
        users.save(new User("cq_alice", "h"));
        users.save(new User("cq_bob", "h"));
        chat.sendDirectMessage("cq_alice", new SendDirectMessageRequest("cq_bob", "hello bob"));

        List<ConversationSummaryResponse> aliceConversations = chat.listConversations("cq_alice");
        assertThat(aliceConversations).hasSize(1);

        ConversationSummaryResponse dm = aliceConversations.get(0);
        assertThat(dm.type()).isEqualTo(ConversationType.DIRECT);
        assertThat(dm.displayName()).isEqualTo("cq_bob");
        assertThat(dm.myRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(dm.lastMessagePreview()).isEqualTo("hello bob");
        assertThat(dm.lastMessageSender()).isEqualTo("cq_alice");
    }

    @Test
    void listMembersReturnsRosterForMemberAndRejectsNonMembers() {
        users.save(new User("cq_admin", "h"));
        users.save(new User("cq_member", "h"));
        users.save(new User("cq_outsider", "h"));
        GroupResponse group = groups.createGroup("cq_admin",
                new CreateGroupRequest("cq team", List.of("cq_member")));

        List<GroupMemberResponse> roster = groups.listMembers("cq_admin", group.conversationId());
        assertThat(roster).extracting(GroupMemberResponse::username)
                .containsExactlyInAnyOrder("cq_admin", "cq_member");

        assertThatThrownBy(() -> groups.listMembers("cq_outsider", group.conversationId()))
                .isInstanceOf(ForbiddenAccessException.class);
    }
}
