package com.nexus.chat;

import com.nexus.chat.dto.AddMemberRequest;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.GroupResponse;
import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendGroupMessageRequest;
import com.nexus.common.ConflictException;
import com.nexus.common.ForbiddenAccessException;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GroupServiceTest {

    @Autowired private GroupService groups;
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
}
