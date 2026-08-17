package com.shortlink.analytics.controller;

import com.shortlink.analytics.ClickCountResponse;
import com.shortlink.analytics.dto.AnalyticsOverviewResponse;
import com.shortlink.analytics.dto.DistributionItem;
import com.shortlink.analytics.dto.LinkAnalyticsResponse;
import com.shortlink.analytics.dto.TimeSeriesPoint;
import com.shortlink.analytics.service.AnalyticsService;
import com.shortlink.analytics.util.AnalyticsParserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;

// REST controller exposing user dashboard and individual short URL analytics endpoints with client timezone conversion.
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // --- User Dashboard Analytics ---

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getUserOverview(
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestHeader(value = "X-Timezone", required = false) String timeZoneHeader) {
        ZoneId userZone = AnalyticsParserUtil.resolveUserZone(timeZoneHeader);
        return ResponseEntity.ok(analyticsService.getUserOverview(range, userZone));
    }

    @GetMapping("/clicks")
    public ResponseEntity<List<TimeSeriesPoint>> getUserClickTimeSeries(
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestHeader(value = "X-Timezone", required = false) String timeZoneHeader) {
        ZoneId userZone = AnalyticsParserUtil.resolveUserZone(timeZoneHeader);
        return ResponseEntity.ok(analyticsService.getUserClickTimeSeries(range, userZone));
    }

    // --- Individual Link Analytics ---

    @GetMapping("/urls/{id}")
    public ResponseEntity<LinkAnalyticsResponse> getLinkAnalytics(
            @PathVariable Long id,
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestHeader(value = "X-Timezone", required = false) String timeZoneHeader) {
        ZoneId userZone = AnalyticsParserUtil.resolveUserZone(timeZoneHeader);
        return ResponseEntity.ok(analyticsService.getLinkAnalytics(id, range, userZone));
    }

    @GetMapping("/urls/{id}/clicks")
    public ResponseEntity<List<TimeSeriesPoint>> getLinkClickTimeSeries(
            @PathVariable Long id,
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestHeader(value = "X-Timezone", required = false) String timeZoneHeader) {
        ZoneId userZone = AnalyticsParserUtil.resolveUserZone(timeZoneHeader);
        return ResponseEntity.ok(analyticsService.getLinkClickTimeSeries(id, range, userZone));
    }

    @GetMapping("/urls/{id}/geography")
    public ResponseEntity<List<DistributionItem>> getLinkGeography(
            @PathVariable Long id,
            @RequestParam(name = "range", defaultValue = "30d") String range) {
        return ResponseEntity.ok(analyticsService.getLinkGeography(id, range));
    }

    @GetMapping("/urls/{id}/devices")
    public ResponseEntity<List<DistributionItem>> getLinkDevices(
            @PathVariable Long id,
            @RequestParam(name = "range", defaultValue = "30d") String range) {
        return ResponseEntity.ok(analyticsService.getLinkDevices(id, range));
    }

    @GetMapping("/urls/{id}/browsers")
    public ResponseEntity<List<DistributionItem>> getLinkBrowsers(
            @PathVariable Long id,
            @RequestParam(name = "range", defaultValue = "30d") String range) {
        return ResponseEntity.ok(analyticsService.getLinkBrowsers(id, range));
    }

    @GetMapping("/urls/{id}/referrers")
    public ResponseEntity<List<DistributionItem>> getLinkReferrers(
            @PathVariable Long id,
            @RequestParam(name = "range", defaultValue = "30d") String range) {
        return ResponseEntity.ok(analyticsService.getLinkReferrers(id, range));
    }

    // --- Legacy / Simple Click Count ---

    @GetMapping("/{shortCode}/clicks")
    public ResponseEntity<ClickCountResponse> getClickCount(@PathVariable String shortCode) {
        long totalClicks = analyticsService.getTotalClicks(shortCode);
        return ResponseEntity.ok(new ClickCountResponse(shortCode, totalClicks));
    }
}
