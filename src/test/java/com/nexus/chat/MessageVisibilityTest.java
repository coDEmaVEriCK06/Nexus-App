package com.nexus.chat;

import com.nexus.AbstractIntegrationTest;
import com.nexus.chat.dto.AddMemberRequest;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.GroupResponse;
import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendGroupMessageRequest;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MessageVisibilityTest extends AbstractIntegrationTest {

    @Autowired private ChatService chat;
    @Autowired private GroupService groups;
    @Autowired private UserRepository users;

    @Test
    void memberAddedLaterCannotSeeOrCountMessagesFromBeforeTheyJoined() {
        users.save(new User("mv_admin", "h"));
        users.save(new User("mv_bob", "h"));
        users.save(new User("mv_carol", "h"));

        GroupResponse group = groups.createGroup("mv_admin",
                new CreateGroupRequest("mv team", List.of("mv_bob")));
        Long gid = group.conversationId();

        // a message is posted while only admin and bob are members
        groups.postGroupMessage("mv_admin", gid, new SendGroupMessageRequest("secret before carol"));

        // carol is added afterwards
        groups.addMember("mv_admin", gid, new AddMemberRequest("mv_carol"));

        // carol's message view excludes everything from before she joined,
        // but includes the system notice announcing she was added
        List<MessageResponse> carolView =
                chat.getConversationMessages("mv_carol", gid, 0, 50).content();
        assertThat(carolView).extracting(MessageResponse::content)
                .doesNotContain("secret before carol")
                .doesNotContain("mv_admin created the group")
                .contains("mv_admin added mv_carol");

        // and the pre-join message is not counted as unread for carol
        long carolUnread = chat.listConversations("mv_carol").get(0).unreadCount();
        assertThat(carolUnread).isEqualTo(1); // only the "added carol" notice

        // sanity: an original member still sees the full history
        List<MessageResponse> bobView =
                chat.getConversationMessages("mv_bob", gid, 0, 50).content();
        assertThat(bobView).extracting(MessageResponse::content)
                .contains("secret before carol");
    }
}
