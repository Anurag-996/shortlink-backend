package com.shortlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuration properties binding frontend client origin URLs.
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
    String url
) {}
