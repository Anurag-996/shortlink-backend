package com.shortlink.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// REST controller exposing click analytics query endpoints.
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // GET /api/analytics/{shortCode}/clicks - Retrieves total click count for a short code.
    @GetMapping("/{shortCode}/clicks")
    public ResponseEntity<ClickCountResponse> getClickCount(@PathVariable String shortCode) {
        long totalClicks = analyticsService.getTotalClicks(shortCode);
        return ResponseEntity.ok(new ClickCountResponse(shortCode, totalClicks));
    }
}
