package com.nexus.chat.dto;

import com.nexus.chat.ConversationType;
import com.nexus.chat.MemberRole;
import com.nexus.chat.MessageType;

import java.time.OffsetDateTime;

/**
 * One row of a user's conversation list (their "inbox"). For a group, {@code displayName}
 * is the group name; for a direct conversation it is the other participant's username.
 * The last-message fields are null for a conversation that has no messages yet, and are
 * used to sort most-recently-active first. {@code lastMessageType} lets the client render a
 * system notice ("Carol promoted Bob") without a redundant sender prefix.
 */
public record ConversationSummaryResponse(
        Long conversationId,
        ConversationType type,
        String displayName,
        MemberRole myRole,
        String lastMessagePreview,
        String lastMessageSender,
        OffsetDateTime lastMessageAt,
        MessageType lastMessageType
) {}
