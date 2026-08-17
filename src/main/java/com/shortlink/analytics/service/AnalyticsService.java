package com.shortlink.analytics.service;

import com.shortlink.analytics.dto.AnalyticsOverviewResponse;
import com.shortlink.analytics.dto.DistributionItem;
import com.shortlink.analytics.dto.LinkAnalyticsResponse;
import com.shortlink.analytics.dto.TimeSeriesPoint;
import com.shortlink.entity.Url;

import java.time.ZoneId;
import java.util.List;

// Service interface for click tracking and user link analytics operations.
public interface AnalyticsService {

    // Asynchronously or safely records a raw click event during redirection with location info.
    void recordClick(Url url, String ipAddress, String userAgent, String referer, String country, String region, String city);

    // Asynchronously or safely records a raw click event during redirection.
    void recordClick(Url url, String ipAddress, String userAgent, String referer);

    // Retrieves dashboard overview metrics for the authenticated user with timezone awareness.
    AnalyticsOverviewResponse getUserOverview(String range, ZoneId userZone);
    AnalyticsOverviewResponse getUserOverview(String range);

    // Retrieves time-series clicks for all links owned by the authenticated user with timezone awareness.
    List<TimeSeriesPoint> getUserClickTimeSeries(String range, ZoneId userZone);
    List<TimeSeriesPoint> getUserClickTimeSeries(String range);

    // Retrieves comprehensive analytics for a specific link with timezone awareness.
    LinkAnalyticsResponse getLinkAnalytics(Long urlId, String range, ZoneId userZone);
    LinkAnalyticsResponse getLinkAnalytics(Long urlId, String range);

    // Retrieves time-series clicks for a specific link.
    List<TimeSeriesPoint> getLinkClickTimeSeries(Long urlId, String range, ZoneId userZone);
    List<TimeSeriesPoint> getLinkClickTimeSeries(Long urlId, String range);

    // Retrieves country geographic distribution for a specific link.
    List<DistributionItem> getLinkGeography(Long urlId, String range);

    // Retrieves device breakdown for a specific link.
    List<DistributionItem> getLinkDevices(Long urlId, String range);

    // Retrieves browser breakdown for a specific link.
    List<DistributionItem> getLinkBrowsers(Long urlId, String range);

    // Retrieves referrer / traffic sources for a specific link.
    List<DistributionItem> getLinkReferrers(Long urlId, String range);

    // Gets simple total click count for a short code.
    long getTotalClicks(String shortCode);
}
