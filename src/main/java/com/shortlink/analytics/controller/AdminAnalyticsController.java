package com.shortlink.analytics.controller;

import com.shortlink.analytics.dto.AdminGrowthPoint;
import com.shortlink.analytics.dto.AdminOverviewResponse;
import com.shortlink.analytics.dto.AdminRecentActivityResponse;
import com.shortlink.analytics.dto.AdminTopLinkResponse;
import com.shortlink.analytics.dto.AdminTopUserResponse;
import com.shortlink.analytics.dto.DistributionItem;
import com.shortlink.analytics.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST controller exposing platform-wide analytics for administrators.
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> getOverview(
            @RequestParam(name = "range", defaultValue = "30d") String range) {
        return ResponseEntity.ok(adminAnalyticsService.getOverview(range));
    }

    @GetMapping("/growth")
    public ResponseEntity<List<AdminGrowthPoint>> getGrowth(
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestParam(name = "metric", defaultValue = "clicks") String metric) {
        return ResponseEntity.ok(adminAnalyticsService.getGrowth(range, metric));
    }

    @GetMapping("/top-links")
    public ResponseEntity<List<AdminTopLinkResponse>> getTopLinks(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminAnalyticsService.getTopLinks(limit));
    }

    @GetMapping("/top-users")
    public ResponseEntity<List<AdminTopUserResponse>> getTopUsers(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminAnalyticsService.getTopUsers(limit));
    }

    @GetMapping("/geography")
    public ResponseEntity<List<DistributionItem>> getGeography(
            @RequestParam(name = "range", defaultValue = "30d") String range) {
        return ResponseEntity.ok(adminAnalyticsService.getPlatformGeography(range));
    }

    @GetMapping("/devices")
    public ResponseEntity<List<DistributionItem>> getDevices(
            @RequestParam(name = "range", defaultValue = "30d") String range) {
        return ResponseEntity.ok(adminAnalyticsService.getPlatformDevices(range));
    }

    @GetMapping("/activity")
    public ResponseEntity<List<AdminRecentActivityResponse>> getRecentActivity() {
        return ResponseEntity.ok(adminAnalyticsService.getRecentActivity());
    }
}
