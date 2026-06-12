package com.nexus.auth.dto;

import java.time.OffsetDateTime;

public record UserResponse(Long id, String username, OffsetDateTime createdAt) {}
