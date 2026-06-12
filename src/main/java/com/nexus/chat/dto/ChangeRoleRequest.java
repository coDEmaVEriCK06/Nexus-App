package com.nexus.chat.dto;

import com.nexus.chat.MemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(

        @NotNull
        MemberRole role
) {}
