package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Summary metrics response for the authenticated user's dashboard.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {
    private long totalLinks;
    private long totalClicks;
    private long uniqueVisitors;
    private long activeLinks;
}
