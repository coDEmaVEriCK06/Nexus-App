package com.nexus.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendGroupMessageRequest(

        @NotBlank
        @Size(max = 8000)
        String content
) {}
