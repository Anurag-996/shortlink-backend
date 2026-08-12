package com.shortlink.dto.redis;

import java.time.LocalDateTime;

// Cache DTO representing a cached short URL entry with expiration metadata.
public record CachedUrl(
    String originalUrl,
    LocalDateTime expiresAt
) {
    // Returns true if the URL has an expiration date and is at or before the current time.
    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(LocalDateTime.now());
    }
}
