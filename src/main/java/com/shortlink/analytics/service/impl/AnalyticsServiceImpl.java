package com.shortlink.analytics.service.impl;

import com.shortlink.analytics.dto.AnalyticsOverviewResponse;
import com.shortlink.analytics.dto.DayOfWeekDistribution;
import com.shortlink.analytics.dto.DistributionItem;
import com.shortlink.analytics.dto.InsightItem;
import com.shortlink.analytics.dto.LinkAnalyticsResponse;
import com.shortlink.analytics.dto.TimeOfDayDistribution;
import com.shortlink.analytics.dto.TimeSeriesPoint;
import com.shortlink.analytics.entity.ClickEvent;
import com.shortlink.analytics.repository.ClickEventRepository;
import com.shortlink.analytics.service.AnalyticsService;
import com.shortlink.analytics.util.AnalyticsParserUtil;
import com.shortlink.entity.Url;
import com.shortlink.exception.UrlNotFoundException;
import com.shortlink.repository.UrlRepository;
import com.shortlink.security.util.SecurityUtils;
import com.shortlink.user.Role;
import com.shortlink.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Implementation of AnalyticsService providing user and link analytics aggregations.
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    @Override
    @Transactional
    public void recordClick(Url url, String ipAddress, String userAgent, String referer, String country, String region, String city) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String deviceType = AnalyticsParserUtil.classifyDevice(userAgent);
            String browser = AnalyticsParserUtil.classifyBrowser(userAgent);
            String os = AnalyticsParserUtil.classifyOperatingSystem(userAgent);
            String refererSource = AnalyticsParserUtil.classifyRefererSource(referer);
            String visitorHash = AnalyticsParserUtil.generateVisitorHash(ipAddress, userAgent);

            ClickEvent event = ClickEvent.builder()
                    .shortUrl(url)
                    .shortCode(url.getShortCode())
                    .clickedAt(now)
                    .ipAddress(ipAddress)
                    .visitorHash(visitorHash)
                    .userAgent(userAgent)
                    .referer(referer)
                    .refererSource(refererSource)
                    .deviceType(deviceType)
                    .browser(browser)
                    .operatingSystem(os)
                    .country(country != null && !country.isBlank() ? country : "UNKNOWN")
                    .region(region != null && !region.isBlank() ? region : "UNKNOWN")
                    .city(city != null && !city.isBlank() ? city : "UNKNOWN")
                    .build();

            clickEventRepository.save(event);
            log.debug("Recorded click event for shortCode={} with country={} city={}", url.getShortCode(), country, city);
        } catch (Exception e) {
            log.error("Failed to record click event for shortCode {}: {}", url.getShortCode(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void recordClick(Url url, String ipAddress, String userAgent, String referer) {
        recordClick(url, ipAddress, userAgent, referer, "UNKNOWN", "UNKNOWN", "UNKNOWN");
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getUserOverview(String range) {
        User user = SecurityUtils.getCurrentUser();
        List<Url> userUrls = getAccessibleUrlsForUser(user);
        if (userUrls.isEmpty()) {
            return AnalyticsOverviewResponse.builder()
                    .totalLinks(0)
                    .totalClicks(0)
                    .uniqueVisitors(0)
                    .activeLinks(0)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = parseSinceDate(range);

        long totalLinks = userUrls.size();
        long activeLinks = userUrls.stream()
                .filter(u -> u.getExpiresAt() == null || u.getExpiresAt().isAfter(now))
                .count();

        long totalClicks = clickEventRepository.countByShortUrlInAndClickedAtAfter(userUrls, since);
        long uniqueVisitors = clickEventRepository.countUniqueVisitorsByShortUrlInAndClickedAtAfter(userUrls, since);

        return AnalyticsOverviewResponse.builder()
                .totalLinks(totalLinks)
                .totalClicks(totalClicks)
                .uniqueVisitors(uniqueVisitors)
                .activeLinks(activeLinks)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getUserClickTimeSeries(String range) {
        User user = SecurityUtils.getCurrentUser();
        List<Url> userUrls = getAccessibleUrlsForUser(user);
        if (userUrls.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime since = parseSinceDate(range);
        List<Object[]> rawRows = clickEventRepository.getTimeSeriesDailyByShortUrlIn(userUrls, since);
        return mapToContinuousDailyTimeSeries(rawRows, since, LocalDate.now());
    }

    private List<Url> getAccessibleUrlsForUser(User user) {
        if (user != null && user.getRole() == Role.ADMIN) {
            return urlRepository.findAll();
        }
        return urlRepository.findAllByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LinkAnalyticsResponse getLinkAnalytics(Long urlId, String range) {
        Url url = findAndAuthorizeUrl(urlId);
        LocalDateTime since = parseSinceDate(range);
        LocalDateTime now = LocalDateTime.now();

        boolean isActive = url.getExpiresAt() == null || url.getExpiresAt().isAfter(now);
        String status = isActive ? "Active" : "Expired";

        long totalClicks = clickEventRepository.countByShortUrlAndClickedAtAfter(url, since);
        long uniqueVisitors = clickEventRepository.countUniqueVisitorsByShortUrlAndClickedAtAfter(url, since);

        long days = Math.max(1, ChronoUnit.DAYS.between(since.toLocalDate(), LocalDate.now()) + 1);
        double avgClicksPerDay = roundToOneDecimal((double) totalClicks / days);

        List<TimeSeriesPoint> timeSeries = "24h".equalsIgnoreCase(range)
                ? mapToHourlyTimeSeries(clickEventRepository.getTimeSeriesHourlyByShortUrl(url, since), since)
                : mapToContinuousDailyTimeSeries(clickEventRepository.getTimeSeriesDailyByShortUrl(url, since), since, LocalDate.now());

        List<DistributionItem> topCountries = mapToDistributionItems(clickEventRepository.getCountryDistributionByShortUrl(url, since), totalClicks);
        List<DistributionItem> topCities = mapToDistributionItems(clickEventRepository.getCityDistributionByShortUrl(url, since), totalClicks);
        List<DistributionItem> devices = mapToDistributionItems(clickEventRepository.getDeviceDistributionByShortUrl(url, since), totalClicks);
        List<DistributionItem> browsers = mapToDistributionItems(clickEventRepository.getBrowserDistributionByShortUrl(url, since), totalClicks);
        List<DistributionItem> operatingSystems = mapToDistributionItems(clickEventRepository.getOsDistributionByShortUrl(url, since), totalClicks);
        List<DistributionItem> referrers = mapToDistributionItems(clickEventRepository.getRefererDistributionByShortUrl(url, since), totalClicks);

        List<TimeOfDayDistribution> hourlyDistribution = mapToHourlyDistribution(clickEventRepository.getHourlyDistributionByShortUrl(url, since), totalClicks);
        List<DayOfWeekDistribution> dayOfWeekDistribution = mapToDayOfWeekDistribution(clickEventRepository.getDayOfWeekDistributionByShortUrl(url, since), totalClicks);

        List<InsightItem> insights = generateInsights(totalClicks, uniqueVisitors, devices, topCountries, referrers, hourlyDistribution, dayOfWeekDistribution);

        return LinkAnalyticsResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .status(status)
                .totalClicks(totalClicks)
                .uniqueVisitors(uniqueVisitors)
                .avgClicksPerDay(avgClicksPerDay)
                .timeSeries(timeSeries)
                .topCountries(topCountries)
                .topCities(topCities)
                .devices(devices)
                .browsers(browsers)
                .operatingSystems(operatingSystems)
                .referrers(referrers)
                .hourlyDistribution(hourlyDistribution)
                .dayOfWeekDistribution(dayOfWeekDistribution)
                .insights(insights)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getLinkClickTimeSeries(Long urlId, String range) {
        Url url = findAndAuthorizeUrl(urlId);
        LocalDateTime since = parseSinceDate(range);
        if ("24h".equalsIgnoreCase(range)) {
            return mapToHourlyTimeSeries(clickEventRepository.getTimeSeriesHourlyByShortUrl(url, since), since);
        }
        return mapToContinuousDailyTimeSeries(clickEventRepository.getTimeSeriesDailyByShortUrl(url, since), since, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionItem> getLinkGeography(Long urlId, String range) {
        Url url = findAndAuthorizeUrl(urlId);
        LocalDateTime since = parseSinceDate(range);
        long totalClicks = clickEventRepository.countByShortUrlAndClickedAtAfter(url, since);
        return mapToDistributionItems(clickEventRepository.getCountryDistributionByShortUrl(url, since), totalClicks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionItem> getLinkDevices(Long urlId, String range) {
        Url url = findAndAuthorizeUrl(urlId);
        LocalDateTime since = parseSinceDate(range);
        long totalClicks = clickEventRepository.countByShortUrlAndClickedAtAfter(url, since);
        return mapToDistributionItems(clickEventRepository.getDeviceDistributionByShortUrl(url, since), totalClicks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionItem> getLinkBrowsers(Long urlId, String range) {
        Url url = findAndAuthorizeUrl(urlId);
        LocalDateTime since = parseSinceDate(range);
        long totalClicks = clickEventRepository.countByShortUrlAndClickedAtAfter(url, since);
        return mapToDistributionItems(clickEventRepository.getBrowserDistributionByShortUrl(url, since), totalClicks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionItem> getLinkReferrers(Long urlId, String range) {
        Url url = findAndAuthorizeUrl(urlId);
        LocalDateTime since = parseSinceDate(range);
        long totalClicks = clickEventRepository.countByShortUrlAndClickedAtAfter(url, since);
        return mapToDistributionItems(clickEventRepository.getRefererDistributionByShortUrl(url, since), totalClicks);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalClicks(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode).orElse(null);
        if (url == null) {
            return 0;
        }
        return clickEventRepository.countByShortUrl(url);
    }

    // --- Helper Methods ---

    private Url findAndAuthorizeUrl(Long urlId) {
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new UrlNotFoundException("Short URL with ID " + urlId + " not found"));

        User currentUser = SecurityUtils.getCurrentUser();

        // Allow owner or platform ADMIN
        boolean isOwner = url.getUser() != null && url.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view analytics for this link");
        }

        return url;
    }

    private LocalDateTime parseSinceDate(String range) {
        if (range == null || range.isBlank()) {
            return LocalDateTime.now().minusDays(30);
        }
        return switch (range.toLowerCase()) {
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            case "90d" -> LocalDateTime.now().minusDays(90);
            case "1y" -> LocalDateTime.now().minusDays(365);
            case "all" -> LocalDateTime.of(2020, 1, 1, 0, 0);
            default -> LocalDateTime.now().minusDays(30);
        };
    }

    private List<TimeSeriesPoint> mapToContinuousDailyTimeSeries(List<Object[]> rows, LocalDateTime since, LocalDate endDate) {
        Map<String, Long> countMap = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    countMap.put(row[0].toString().trim(), ((Number) row[1]).longValue());
                }
            }
        }

        List<TimeSeriesPoint> points = new ArrayList<>();
        LocalDate current = since.toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (!current.isAfter(endDate)) {
            String dateKey = current.format(formatter);
            points.add(new TimeSeriesPoint(dateKey, countMap.getOrDefault(dateKey, 0L)));
            current = current.plusDays(1);
        }

        return points;
    }

    private List<TimeSeriesPoint> mapToHourlyTimeSeries(List<Object[]> rows, LocalDateTime since) {
        Map<String, Long> countMap = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    countMap.put(row[0].toString().trim(), ((Number) row[1]).longValue());
                }
            }
        }

        List<TimeSeriesPoint> points = new ArrayList<>();
        LocalDateTime current = since.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");

        while (!current.isAfter(now)) {
            String hourKey = current.format(formatter);
            points.add(new TimeSeriesPoint(hourKey, countMap.getOrDefault(hourKey, 0L)));
            current = current.plusHours(1);
        }

        return points;
    }

    private List<DistributionItem> mapToDistributionItems(List<Object[]> rows, long totalClicks) {
        if (rows == null || rows.isEmpty() || totalClicks == 0) {
            return Collections.emptyList();
        }

        List<DistributionItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                String label = row[0].toString();
                long count = ((Number) row[1]).longValue();
                double percentage = roundToOneDecimal((count * 100.0) / totalClicks);
                items.add(new DistributionItem(label, count, percentage));
            }
        }
        return items;
    }

    private List<TimeOfDayDistribution> mapToHourlyDistribution(List<Object[]> rows, long totalClicks) {
        Map<String, Long> hourMap = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    hourMap.put(row[0].toString().trim(), ((Number) row[1]).longValue());
                }
            }
        }

        List<TimeOfDayDistribution> list = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            String hourStr = String.format("%02d", h);
            long count = hourMap.getOrDefault(hourStr, 0L);
            double percentage = totalClicks > 0 ? roundToOneDecimal((count * 100.0) / totalClicks) : 0.0;
            list.add(new TimeOfDayDistribution(hourStr, count, percentage));
        }
        return list;
    }

    private List<DayOfWeekDistribution> mapToDayOfWeekDistribution(List<Object[]> rows, long totalClicks) {
        Map<String, Long> dayMap = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    dayMap.put(row[0].toString().trim().toLowerCase(), ((Number) row[1]).longValue());
                }
            }
        }

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        List<DayOfWeekDistribution> list = new ArrayList<>();
        for (String day : days) {
            long count = dayMap.getOrDefault(day.toLowerCase(), 0L);
            double percentage = totalClicks > 0 ? roundToOneDecimal((count * 100.0) / totalClicks) : 0.0;
            list.add(new DayOfWeekDistribution(day, count, percentage));
        }
        return list;
    }

    private List<InsightItem> generateInsights(long totalClicks, long uniqueVisitors,
                                              List<DistributionItem> devices,
                                              List<DistributionItem> countries,
                                              List<DistributionItem> referrers,
                                              List<TimeOfDayDistribution> hourly,
                                              List<DayOfWeekDistribution> dayOfWeek) {
        List<InsightItem> insights = new ArrayList<>();
        if (totalClicks == 0) {
            return insights;
        }

        // 1. Overall clicks & unique visitor insights
        insights.add(new InsightItem("trend", String.format("Your link received %,d total clicks from %,d unique visitors.", totalClicks, uniqueVisitors)));

        // 2. Top device insight
        if (!devices.isEmpty()) {
            DistributionItem topDevice = devices.get(0);
            if (topDevice.getPercentage() >= 40.0 && !"UNKNOWN".equalsIgnoreCase(topDevice.getLabel())) {
                insights.add(new InsightItem("device", String.format("%.0f%% of visitors use %s devices.", topDevice.getPercentage(), topDevice.getLabel().toLowerCase())));
            }
        }

        // 3. Top location insight
        if (!countries.isEmpty()) {
            DistributionItem topCountry = countries.get(0);
            if (!"UNKNOWN".equalsIgnoreCase(topCountry.getLabel())) {
                insights.add(new InsightItem("location", String.format("Most visitors (%.0f%%) are located in %s.", topCountry.getPercentage(), topCountry.getLabel())));
            }
        }

        // 4. Top referrer insight
        if (!referrers.isEmpty()) {
            DistributionItem topRef = referrers.get(0);
            if (!"Direct".equalsIgnoreCase(topRef.getLabel())) {
                insights.add(new InsightItem("source", String.format("%s is your top traffic source generating %.0f%% of all visits.", topRef.getLabel(), topRef.getPercentage())));
            } else {
                insights.add(new InsightItem("source", String.format("Direct traffic accounts for %.0f%% of visits (bookmarks, messengers, or direct URL entry).", topRef.getPercentage())));
            }
        }

        // 5. Peak day insight
        if (dayOfWeek != null && !dayOfWeek.isEmpty()) {
            DayOfWeekDistribution peakDay = Collections.max(dayOfWeek, (a, b) -> Long.compare(a.getCount(), b.getCount()));
            if (peakDay.getCount() > 0) {
                insights.add(new InsightItem("trend", String.format("Your link experiences peak engagement on %ss.", peakDay.getDay())));
            }
        }

        // 6. Peak hour insight
        if (hourly != null && !hourly.isEmpty()) {
            TimeOfDayDistribution peakHour = Collections.max(hourly, (a, b) -> Long.compare(a.getCount(), b.getCount()));
            if (peakHour.getCount() > 0) {
                insights.add(new InsightItem("time", String.format("Peak engagement time is around %s.", peakHour.getHour())));
            }
        }

        return insights;
    }

    private double roundToOneDecimal(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return 0.0;
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
