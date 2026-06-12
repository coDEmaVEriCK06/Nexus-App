package com.nexus.chat;

import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.common.ForbiddenAccessException;
import com.nexus.common.PageResponse;
import com.nexus.common.ResourceNotFoundException;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final UserRepository users;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;

    public ChatService(UserRepository users,
                       ConversationRepository conversations,
                       ConversationMemberRepository members,
                       MessageRepository messages) {
        this.users = users;
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
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

        return new MessageResponse(
                message.getId(),
                conversation.getId(),
                sender.getUsername(),
                message.getContent(),
                message.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getConversationMessages(String requesterUsername,
                                                                 Long conversationId,
                                                                 int page,
                                                                 int size) {
        User requester = users.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!members.existsByConversationIdAndUserId(conversationId, requester.getId())) {
            throw new ForbiddenAccessException("You are not a member of this conversation");
        }

        Page<MessageResponse> result =
                messages.findResponsesByConversationId(conversationId, PageRequest.of(page, size));
        return PageResponse.from(result);
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
