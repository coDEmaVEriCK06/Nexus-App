package com.nexus.chat;

import com.nexus.chat.dto.MessageResponse;

import java.util.List;

public record MessagePostedEvent(MessageResponse message, List<String> recipientUsernames) {}
