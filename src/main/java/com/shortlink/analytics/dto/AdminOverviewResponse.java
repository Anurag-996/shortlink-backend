package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Platform-wide overview metrics response for ADMIN users.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewResponse {
    // Primary metrics
    private long totalUsers;
    private long totalLinks;
    private long totalClicks;
    private long activeLinks;

    // Secondary metrics
    private long newUsers;
    private long newLinks;
    private long clicksToday;
}
