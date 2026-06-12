package com.nexus.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(

        @NotBlank
        String username
) {}
