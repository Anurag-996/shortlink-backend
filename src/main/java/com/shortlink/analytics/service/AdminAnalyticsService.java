package com.shortlink.analytics.service;

import com.shortlink.analytics.dto.AdminGrowthPoint;
import com.shortlink.analytics.dto.AdminOverviewResponse;
import com.shortlink.analytics.dto.AdminRecentActivityResponse;
import com.shortlink.analytics.dto.AdminTopLinkResponse;
import com.shortlink.analytics.dto.AdminTopUserResponse;
import com.shortlink.analytics.dto.DistributionItem;

import java.util.List;

// Service interface for platform-wide analytics available exclusively to ADMIN users.
public interface AdminAnalyticsService {

    // Retrieves primary platform metrics (users, links, clicks, active links, new metrics).
    AdminOverviewResponse getOverview(String range);

    // Retrieves growth time-series data for clicks, new users, or new links.
    List<AdminGrowthPoint> getGrowth(String range, String metric);

    // Retrieves highest-performing links across the entire platform.
    List<AdminTopLinkResponse> getTopLinks(int limit);

    // Retrieves top users generating the most platform link activity.
    List<AdminTopUserResponse> getTopUsers(int limit);

    // Retrieves platform-wide geographic distribution of visitors.
    List<DistributionItem> getPlatformGeography(String range);

    // Retrieves platform-wide device breakdown.
    List<DistributionItem> getPlatformDevices(String range);

    // Retrieves recent platform activity events.
    List<AdminRecentActivityResponse> getRecentActivity();
}
