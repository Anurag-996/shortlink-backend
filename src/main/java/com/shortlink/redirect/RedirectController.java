package com.shortlink.redirect;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.shortlink.analytics.service.AnalyticsService;
import com.shortlink.analytics.util.GeoLocationResolver;
import com.shortlink.analytics.util.GeoLocationResolver.GeoLocation;
import com.shortlink.repository.UrlRepository;
import com.shortlink.service.UrlService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// REST Controller handling short URL redirection and tracking click analytics.
@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;
    private final UrlRepository urlRepository;
    private final AnalyticsService analyticsService;

    // GET /{shortCode} - Redirects short code to original target URL and records click analytics event. Returns HTTP 302 Found.
    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        String referrer = request.getHeader(HttpHeaders.REFERER);
        GeoLocation geo = GeoLocationResolver.resolve(request, ipAddress);

        String originalUrl = urlService.getOriginalUrlAndIncrementClick(shortCode);

        // Record click analytics event directly and non-blockingly
        try {
            urlRepository.findByShortCode(shortCode).ifPresent(url ->
                    analyticsService.recordClick(url, ipAddress, userAgent, referrer, geo.country(), geo.region(), geo.city())
            );
        } catch (Exception e) {
            log.error("Failed to record click analytics for shortCode {}: {}", shortCode, e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
