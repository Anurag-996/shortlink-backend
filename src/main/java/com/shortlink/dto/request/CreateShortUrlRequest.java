package com.shortlink.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// Request DTO for creating a new short URL supporting both full URLs and raw domains (e.g. youtube.com).
public record CreateShortUrlRequest(
    @NotBlank(message = "Original URL must not be blank")
    @Pattern(
        regexp = "^(https?://)?(localhost|127\\.0\\.0\\.1|([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}|(\\d{1,3}\\.){3}\\d{1,3})(:\\d{1,5})?(/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*)?$",
        message = "Must be a valid URL or domain (e.g. youtube.com, https://example.com, sub.domain.live)"
    )
    String originalUrl,

    @Size(min = 3, max = 16, message = "Custom alias must be between 3 and 16 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]*$",
        message = "Custom alias can only contain letters, numbers, hyphens, and underscores"
    )
    String customAlias,

    @Future(message = "Expiration time must be in the future")
    LocalDateTime expiresAt
) {}
