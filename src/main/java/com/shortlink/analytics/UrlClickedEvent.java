package com.shortlink.analytics;

import java.time.Instant;
import java.util.UUID;

// Immutable event payload created on URL click.
public record UrlClickedEvent(
    UUID eventId,
    String shortCode,
    Instant timestamp,
    String ipAddress,
    String userAgent,
    String referrer
) {
    // Convenience constructor generating a unique eventId and current UTC timestamp.
    public static UrlClickedEvent of(String shortCode, String ipAddress, String userAgent, String referrer) {
        return new UrlClickedEvent(
            UUID.randomUUID(),
            shortCode,
            Instant.now(),
            ipAddress,
            userAgent,
            referrer
        );
    }
}
