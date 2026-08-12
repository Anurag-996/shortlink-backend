package com.shortlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuration properties binding JWT secret and token lifetimes.
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long accessTokenExpiration,
    long refreshTokenExpiration
) {
    public JwtProperties {
        if (accessTokenExpiration <= 0) {
            accessTokenExpiration = 900_000L; // 15 minutes default
        }
        if (refreshTokenExpiration <= 0) {
            refreshTokenExpiration = 2_592_000_000L; // 30 days default
        }
    }
}
