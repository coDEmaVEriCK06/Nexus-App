package com.nexus.chat;

import com.nexus.chat.dto.AddMemberRequest;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.GroupMemberResponse;
import com.nexus.chat.dto.GroupResponse;
import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendGroupMessageRequest;
import com.nexus.common.ConflictException;
import com.nexus.common.ForbiddenAccessException;
import com.nexus.common.ResourceNotFoundException;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final UserRepository users;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;

    public GroupService(UserRepository users,
                        ConversationRepository conversations,
                        ConversationMemberRepository members,
                        MessageRepository messages) {
        this.users = users;
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
    }

    @Transactional
    public GroupResponse createGroup(String creatorUsername, CreateGroupRequest request) {
        User creator = loadUser(creatorUsername);
        Conversation group = conversations.save(new Conversation(ConversationType.GROUP, request.name()));
        members.save(new ConversationMember(group, creator, MemberRole.ADMIN));

        if (request.memberUsernames() != null) {
            for (String username : request.memberUsernames()) {
                if (username.equals(creatorUsername)) {
                    continue;
                }
                User member = loadUser(username);
                if (!members.existsByConversationIdAndUserId(group.getId(), member.getId())) {
                    members.save(new ConversationMember(group, member, MemberRole.MEMBER));
                }
            }
        }
        return toGroupResponse(group);
    }

    @Transactional
    public void addMember(String actorUsername, Long conversationId, AddMemberRequest request) {
        requireAdmin(actorUsername, conversationId);
        User toAdd = loadUser(request.username());
        if (members.existsByConversationIdAndUserId(conversationId, toAdd.getId())) {
            throw new ConflictException("User is already a member of this group");
        }
        Conversation group = conversations.getReferenceById(conversationId);
        members.save(new ConversationMember(group, toAdd, MemberRole.MEMBER));
    }

    @Transactional
    public void removeMember(String actorUsername, Long conversationId, String targetUsername) {
        requireAdmin(actorUsername, conversationId);
        User target = loadUser(targetUsername);
        ConversationMember membership = members.findByConversationIdAndUserId(conversationId, target.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this group"));
        members.delete(membership);
    }

    @Transactional
    public MessageResponse postGroupMessage(String senderUsername, Long conversationId,
                                            SendGroupMessageRequest request) {
        User sender = loadUser(senderUsername);
        if (!members.existsByConversationIdAndUserId(conversationId, sender.getId())) {
            throw new ForbiddenAccessException("You are not a member of this conversation");
        }
        Conversation conversation = conversations.getReferenceById(conversationId);
        Message message = messages.save(new Message(conversation, sender, request.content()));
        return new MessageResponse(
                message.getId(),
                conversationId,
                sender.getUsername(),
                message.getContent(),
                message.getCreatedAt());
    }

    private void requireAdmin(String actorUsername, Long conversationId) {
        User actor = loadUser(actorUsername);
        if (!members.existsByConversationIdAndUserIdAndRole(conversationId, actor.getId(), MemberRole.ADMIN)) {
            throw new ForbiddenAccessException("Only a group admin can perform this action");
        }
    }

    private User loadUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private GroupResponse toGroupResponse(Conversation group) {
        List<GroupMemberResponse> memberList = members.findMembersByConversationId(group.getId());
        return new GroupResponse(group.getId(), group.getName(), memberList);
    }
}
