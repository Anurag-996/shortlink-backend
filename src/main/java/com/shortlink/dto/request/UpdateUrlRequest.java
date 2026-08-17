package com.shortlink.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

// Request DTO for updating an existing short URL's destination, optional expiration, and optional analytics reset.
public record UpdateUrlRequest(
    @NotBlank(message = "Original URL must not be blank")
    @Pattern(
        regexp = "^(https?://)?(localhost|127\\.0\\.0\\.1|([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}|(\\d{1,3}\\.){3}\\d{1,3})(:\\d{1,5})?(/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*)?$",
        message = "Must be a valid URL or domain (e.g. youtube.com, https://example.com, sub.domain.live)"
    )
    String originalUrl,

    @Future(message = "Expiration time must be in the future")
    LocalDateTime expiresAt,

    Boolean resetAnalytics
) {}
