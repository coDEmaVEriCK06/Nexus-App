package com.nexus.chat;

import com.nexus.chat.dto.ConversationSummaryResponse;
import com.nexus.chat.dto.GroupMemberResponse;
import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.common.ForbiddenAccessException;
import com.nexus.common.PageResponse;
import com.nexus.common.ResourceNotFoundException;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final UserRepository users;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;
    private final ApplicationEventPublisher events;

    public ChatService(UserRepository users,
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
    public MessageResponse sendDirectMessage(String senderUsername, SendDirectMessageRequest request) {
        User sender = users.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User recipient = users.findByUsername(request.recipientUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recipient not found: " + request.recipientUsername()));

        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("You cannot send a direct message to yourself");
        }

        Conversation conversation = getOrCreateDirectConversation(sender, recipient);
        Message message = messages.save(new Message(conversation, sender, request.content()));
        conversation.applyLastMessage(message);

        MessageResponse response = new MessageResponse(
                message.getId(),
                conversation.getId(),
                sender.getUsername(),
                message.getContent(),
                message.getCreatedAt(),
                message.getType());

        events.publishEvent(new MessagePostedEvent(response, recipientUsernames(conversation.getId())));
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getConversationMessages(String requesterUsername,
                                                                 Long conversationId,
                                                                 int page,
                                                                 int size) {
        User requester = users.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ConversationMember membership = members.findByConversationIdAndUserId(conversationId, requester.getId())
                .orElseThrow(() -> new ForbiddenAccessException("You are not a member of this conversation"));

        // A member only sees messages from their join point onward — no pre-join history.
        Page<MessageResponse> result = messages.findResponsesByConversationId(
                conversationId, membership.getJoinedAt(), PageRequest.of(page, size));
        return PageResponse.from(result);
    }

    /**
     * Lists every conversation the user belongs to (direct and group), most-recently-active
     * first. The last-message fields come from the denormalized snapshot on each conversation
     * (maintained on write), and direct-conversation peer names are resolved in a single batched
     * query — so the whole list is served in a fixed number of queries regardless of how many
     * conversations the user has (no per-conversation N+1).
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listConversations(String username) {
        User me = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ConversationMember> memberships =
                members.findMembershipsWithConversationByUserId(me.getId());
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> directIds = memberships.stream()
                .map(ConversationMember::getConversation)
                .filter(c -> c.getType() == ConversationType.DIRECT)
                .map(Conversation::getId)
                .toList();

        Map<Long, String> peerByConversation = new HashMap<>();
        if (!directIds.isEmpty()) {
            for (Object[] row : members.findPeerUsernames(directIds, me.getId())) {
                peerByConversation.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }

        List<Long> allIds = memberships.stream()
                .map(m -> m.getConversation().getId())
                .toList();
        Map<Long, Long> unreadByConversation = new HashMap<>();
        for (Object[] row : messages.countUnreadByConversation(me.getId(), allIds)) {
            unreadByConversation.put((Long) row[0], (Long) row[1]);
        }

        List<ConversationSummaryResponse> summaries = new ArrayList<>();
        for (ConversationMember membership : memberships) {
            Conversation conversation = membership.getConversation();
            String displayName = conversation.getType() == ConversationType.GROUP
                    ? conversation.getName()
                    : peerByConversation.getOrDefault(conversation.getId(), "(unknown)");

            summaries.add(new ConversationSummaryResponse(
                    conversation.getId(),
                    conversation.getType(),
                    displayName,
                    membership.getRole(),
                    conversation.getLastMessagePreview(),
                    conversation.getLastMessageSender(),
                    conversation.getLastMessageAt(),
                    conversation.getLastMessageType(),
                    unreadByConversation.getOrDefault(conversation.getId(), 0L)));
        }

        summaries.sort(Comparator.comparing(
                ConversationSummaryResponse::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return summaries;
    }

    /**
     * Marks a conversation read for the user by advancing their read watermark to the newest
     * message. Idempotent. Requires the user to be a member.
     */
    @Transactional
    public void markConversationRead(String username, Long conversationId) {
        User me = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ConversationMember membership = members.findByConversationIdAndUserId(conversationId, me.getId())
                .orElseThrow(() -> new ForbiddenAccessException("You are not a member of this conversation"));
        Long latest = messages.findMaxMessageId(conversationId);
        if (latest != null) {
            membership.setLastReadMessageId(latest);
            members.save(membership);
        }
    }

    private List<String> recipientUsernames(Long conversationId) {
        return members.findMembersByConversationId(conversationId).stream()
                .map(GroupMemberResponse::username)
                .toList();
    }

    private Conversation getOrCreateDirectConversation(User a, User b) {
        String key = directKey(a.getId(), b.getId());
        return conversations.findByDirectKey(key).orElseGet(() -> {
            Conversation conversation = new Conversation(ConversationType.DIRECT, null);
            conversation.setDirectKey(key);
            Conversation saved = conversations.save(conversation);
            members.save(new ConversationMember(saved, a, MemberRole.MEMBER));
            members.save(new ConversationMember(saved, b, MemberRole.MEMBER));
            return saved;
        });
    }

    private static String directKey(Long a, Long b) {
        return Math.min(a, b) + "_" + Math.max(a, b);
    }
}
