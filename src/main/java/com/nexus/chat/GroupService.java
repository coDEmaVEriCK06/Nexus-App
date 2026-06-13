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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final UserRepository users;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;
    private final ApplicationEventPublisher events;

    public GroupService(UserRepository users,
                        ConversationRepository conversations,
                        ConversationMemberRepository members,
                        MessageRepository messages,
                        ApplicationEventPublisher events) {
        this.users = users;
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.events = events;
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
        requireGroup(conversationId);
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
        requireGroup(conversationId);
        requireAdmin(actorUsername, conversationId);
        ConversationMember membership = loadMembership(conversationId, loadUser(targetUsername));
        if (membership.getRole() == MemberRole.ADMIN && isLastAdminWithOthersRemaining(conversationId)) {
            throw new ConflictException("Cannot remove the last admin while other members remain");
        }
        members.delete(membership);
    }

    @Transactional
    public void leaveGroup(String username, Long conversationId) {
        requireGroup(conversationId);
        ConversationMember membership = loadMembership(conversationId, loadUser(username));
        if (membership.getRole() == MemberRole.ADMIN && isLastAdminWithOthersRemaining(conversationId)) {
            throw new ConflictException("Promote another member to admin before leaving the group");
        }
        members.delete(membership);
    }

    @Transactional
    public void changeRole(String actorUsername, Long conversationId, String targetUsername, MemberRole newRole) {
        requireGroup(conversationId);
        requireAdmin(actorUsername, conversationId);
        ConversationMember membership = loadMembership(conversationId, loadUser(targetUsername));
        if (membership.getRole() == newRole) {
            return;
        }
        if (newRole == MemberRole.MEMBER
                && members.countByConversationIdAndRole(conversationId, MemberRole.ADMIN) == 1) {
            throw new ConflictException("Cannot demote the last admin of the group");
        }
        membership.setRole(newRole);
        members.save(membership);
    }

    @Transactional
    public MessageResponse postGroupMessage(String senderUsername, Long conversationId,
                                            SendGroupMessageRequest request) {
        requireGroup(conversationId);
        User sender = loadUser(senderUsername);
        if (!members.existsByConversationIdAndUserId(conversationId, sender.getId())) {
            throw new ForbiddenAccessException("You are not a member of this conversation");
        }
        Conversation conversation = conversations.getReferenceById(conversationId);
        Message message = messages.save(new Message(conversation, sender, request.content()));
        MessageResponse response = new MessageResponse(
                message.getId(),
                conversationId,
                sender.getUsername(),
                message.getContent(),
                message.getCreatedAt());

        List<String> recipients = members.findMembersByConversationId(conversationId).stream()
                .map(GroupMemberResponse::username)
                .toList();
        events.publishEvent(new MessagePostedEvent(response, recipients));
        return response;
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> listMembers(String requesterUsername, Long conversationId) {
        requireGroup(conversationId);
        User requester = loadUser(requesterUsername);
        if (!members.existsByConversationIdAndUserId(conversationId, requester.getId())) {
            throw new ForbiddenAccessException("You are not a member of this group");
        }
        return members.findMembersByConversationId(conversationId);
    }

    private void requireGroup(Long conversationId) {
        if (!conversations.existsByIdAndType(conversationId, ConversationType.GROUP)) {
            throw new ResourceNotFoundException("Group not found");
        }
    }

    private void requireAdmin(String actorUsername, Long conversationId) {
        User actor = loadUser(actorUsername);
        if (!members.existsByConversationIdAndUserIdAndRole(conversationId, actor.getId(), MemberRole.ADMIN)) {
            throw new ForbiddenAccessException("Only a group admin can perform this action");
        }
    }

    private boolean isLastAdminWithOthersRemaining(Long conversationId) {
        return members.countByConversationIdAndRole(conversationId, MemberRole.ADMIN) == 1
                && members.countByConversationId(conversationId) > 1;
    }

    private ConversationMember loadMembership(Long conversationId, User user) {
        return members.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this group"));
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
