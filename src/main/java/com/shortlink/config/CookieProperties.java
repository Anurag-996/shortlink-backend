package com.shortlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuration properties binding HttpOnly refresh token cookie settings.
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
    boolean secure,
    String sameSite,
    String path
) {
    public CookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Strict";
        }
        if (path == null || path.isBlank()) {
            path = "/api/auth";
        }
    }
}
