package com.nexus.chat.dto;

import java.util.List;

public record GroupResponse(Long conversationId, String name, List<GroupMemberResponse> members) {}
