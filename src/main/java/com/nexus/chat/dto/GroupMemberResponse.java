package com.nexus.chat.dto;

import com.nexus.chat.MemberRole;

public record GroupMemberResponse(String username, MemberRole role) {}
