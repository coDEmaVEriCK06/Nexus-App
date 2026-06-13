package com.nexus.chat;

import com.nexus.AbstractIntegrationTest;

import com.nexus.chat.dto.AddMemberRequest;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.GroupResponse;
import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.chat.dto.SendGroupMessageRequest;
import com.nexus.common.ConflictException;
import com.nexus.common.ForbiddenAccessException;
import com.nexus.common.ResourceNotFoundException;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GroupServiceTest extends AbstractIntegrationTest {

    @Autowired private GroupService groups;
    @Autowired private ChatService chat;
    @Autowired private UserRepository users;

    @Test
    void groupLifecycleAndAuthorization() {
        users.save(new User("g_alice", "h"));
        users.save(new User("g_bob", "h"));
        users.save(new User("g_carol", "h"));
        users.save(new User("g_dave", "h"));

        GroupResponse group = groups.createGroup("g_alice",
                new CreateGroupRequest("Team", List.of("g_bob")));
        Long conversationId = group.conversationId();
        assertThat(group.members()).hasSize(2);

        groups.addMember("g_alice", conversationId, new AddMemberRequest("g_carol"));

        assertThatThrownBy(() ->
                groups.addMember("g_bob", conversationId, new AddMemberRequest("g_dave")))
                .isInstanceOf(ForbiddenAccessException.class);

        MessageResponse posted = groups.postGroupMessage("g_bob", conversationId,
                new SendGroupMessageRequest("hi team"));
        assertThat(posted.id()).isNotNull();

        assertThatThrownBy(() ->
                groups.postGroupMessage("g_dave", conversationId,
                        new SendGroupMessageRequest("intruder")))
                .isInstanceOf(ForbiddenAccessException.class);

        groups.removeMember("g_alice", conversationId, "g_carol");

        assertThatThrownBy(() ->
                groups.addMember("g_alice", conversationId, new AddMemberRequest("g_bob")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void adminLifecycleAndLastAdminInvariant() {
        users.save(new User("a_owner", "h"));
        users.save(new User("a_bob", "h"));
        users.save(new User("a_carol", "h"));

        GroupResponse group = groups.createGroup("a_owner",
                new CreateGroupRequest("Crew", List.of("a_bob")));
        Long cid = group.conversationId();

        assertThatThrownBy(() -> groups.leaveGroup("a_owner", cid))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> groups.changeRole("a_owner", cid, "a_owner", MemberRole.MEMBER))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> groups.removeMember("a_owner", cid, "a_owner"))
                .isInstanceOf(ConflictException.class);

        groups.changeRole("a_owner", cid, "a_bob", MemberRole.ADMIN);

        groups.leaveGroup("a_owner", cid);

        groups.addMember("a_bob", cid, new AddMemberRequest("a_carol"));
    }

    @Test
    void groupOperationsRejectDirectConversations() {
        users.save(new User("d_alice", "h"));
        users.save(new User("d_bob", "h"));

        MessageResponse dm = chat.sendDirectMessage("d_alice",
                new SendDirectMessageRequest("d_bob", "hi"));
        Long directId = dm.conversationId();

        assertThatThrownBy(() -> groups.leaveGroup("d_alice", directId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> groups.addMember("d_alice", directId, new AddMemberRequest("d_bob")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> groups.postGroupMessage("d_alice", directId,
                new SendGroupMessageRequest("x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
