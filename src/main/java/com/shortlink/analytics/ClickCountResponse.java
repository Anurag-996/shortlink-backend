package com.shortlink.analytics;

// Response DTO returning click count analytics for a short code.
public record ClickCountResponse(
    String shortCode,
    long totalClicks
) {}
