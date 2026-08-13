package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// Comprehensive analytics payload for an individual short link.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAnalyticsResponse {
    private Long id;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String status; // "Active", "Expired"

    // Primary Top Metrics
    private long totalClicks;
    private long uniqueVisitors;
    private double avgClicksPerDay;

    // Visual Visualizations & Breakdowns
    private List<TimeSeriesPoint> timeSeries;
    private List<DistributionItem> topCountries;
    private List<DistributionItem> topCities;
    private List<DistributionItem> devices;
    private List<DistributionItem> browsers;
    private List<DistributionItem> operatingSystems;
    private List<DistributionItem> referrers;
    private List<TimeOfDayDistribution> hourlyDistribution;
    private List<DayOfWeekDistribution> dayOfWeekDistribution;
    private List<InsightItem> insights;
}
