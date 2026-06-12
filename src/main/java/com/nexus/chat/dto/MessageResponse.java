package com.nexus.chat.dto;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        String senderUsername,
        String content,
        OffsetDateTime createdAt
) {}
