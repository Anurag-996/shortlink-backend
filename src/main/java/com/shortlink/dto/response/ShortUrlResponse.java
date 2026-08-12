package com.shortlink.dto.response;

import java.time.LocalDateTime;

// Response DTO representing shortened URL details. Implemented as an immutable Java Record.
public record ShortUrlResponse(
    Long id,
    String originalUrl,
    String shortCode,
    String shortUrl,
    Long clickCount,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {}
