package com.nexus.chat;

import com.nexus.AbstractIntegrationTest;
import com.nexus.chat.dto.AddMemberRequest;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.GroupResponse;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class GroupSystemEventTest extends AbstractIntegrationTest {

    @Autowired private GroupService groups;
    @Autowired private UserRepository users;
    @Autowired private MessageRepository messages;

    private Long newGroupWithBob() {
        users.save(new User("se_admin", "h"));
        users.save(new User("se_bob", "h"));
        GroupResponse g = groups.createGroup("se_admin",
                new CreateGroupRequest("se team", List.of("se_bob")));
        return g.conversationId();
    }

    private List<String> systemTexts(Long conversationId) {
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .filter(m -> m.getType() == MessageType.SYSTEM)
                .map(Message::getContent)
                .toList();
    }

    @Test
    void createAndAddEmitSystemNotices() {
        Long cid = newGroupWithBob();
        users.save(new User("se_carol", "h"));
        groups.addMember("se_admin", cid, new AddMemberRequest("se_carol"));

        assertThat(systemTexts(cid))
                .anyMatch(t -> t.contains("se_admin created the group"))
                .anyMatch(t -> t.contains("se_admin added se_carol"));
    }

    @Test
    void promoteEmitsSystemNotice() {
        Long cid = newGroupWithBob();
        groups.changeRole("se_admin", cid, "se_bob", MemberRole.ADMIN);
        assertThat(systemTexts(cid)).anyMatch(t -> t.contains("promoted se_bob to admin"));
    }

    @Test
    void leaveEmitsSystemNotice() {
        Long cid = newGroupWithBob();
        groups.leaveGroup("se_bob", cid);
        assertThat(systemTexts(cid)).anyMatch(t -> t.equals("se_bob left"));
    }
}
