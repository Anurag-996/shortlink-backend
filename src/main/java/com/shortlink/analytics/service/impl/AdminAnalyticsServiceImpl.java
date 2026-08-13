package com.shortlink.analytics.service.impl;

import com.shortlink.analytics.dto.AdminGrowthPoint;
import com.shortlink.analytics.dto.AdminOverviewResponse;
import com.shortlink.analytics.dto.AdminRecentActivityResponse;
import com.shortlink.analytics.dto.AdminTopLinkResponse;
import com.shortlink.analytics.dto.AdminTopUserResponse;
import com.shortlink.analytics.dto.DistributionItem;
import com.shortlink.analytics.repository.ClickEventRepository;
import com.shortlink.analytics.service.AdminAnalyticsService;
import com.shortlink.entity.Url;
import com.shortlink.repository.UrlRepository;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Implementation of AdminAnalyticsService providing platform-wide aggregations for administrators.
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UserRepository userRepository;
    private final UrlRepository urlRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview(String range) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = parseSinceDate(range);
        Instant sinceInstant = since.toInstant(ZoneOffset.UTC);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long totalUsers = userRepository.countByEnabledTrue();
        long totalLinks = urlRepository.count();
        long totalClicks = clickEventRepository.count();
        long activeLinks = urlRepository.countActiveUrls(now);

        long newUsers = userRepository.countByCreatedAtAfter(sinceInstant);
        long newLinks = urlRepository.countByCreatedAtAfter(since);
        long clicksToday = clickEventRepository.countPlatformClicksAfter(todayStart);

        return AdminOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalLinks(totalLinks)
                .totalClicks(totalClicks)
                .activeLinks(activeLinks)
                .newUsers(newUsers)
                .newLinks(newLinks)
                .clicksToday(clicksToday)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminGrowthPoint> getGrowth(String range, String metric) {
        LocalDateTime since = parseSinceDate(range);
        LocalDate endDate = LocalDate.now();

        List<Object[]> rawRows;
        if ("users".equalsIgnoreCase(metric)) {
            Instant sinceInstant = since.toInstant(ZoneOffset.UTC);
            rawRows = userRepository.getNewUsersTimeSeriesDaily(sinceInstant);
        } else if ("links".equalsIgnoreCase(metric)) {
            rawRows = urlRepository.getNewUrlsTimeSeriesDaily(since);
        } else {
            rawRows = clickEventRepository.getPlatformTimeSeriesDaily(since);
        }

        return mapToContinuousAdminTimeSeries(rawRows, since, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTopLinkResponse> getTopLinks(int limit) {
        int max = limit > 0 ? limit : 10;
        List<Url> urls = urlRepository.findTop10ByOrderByClickCountDesc();
        LocalDateTime now = LocalDateTime.now();

        List<AdminTopLinkResponse> responses = new ArrayList<>();
        int rank = 1;
        for (Url url : urls) {
            if (responses.size() >= max) break;
            boolean isActive = url.getExpiresAt() == null || url.getExpiresAt().isAfter(now);
            String owner = (url.getUser() != null) ? url.getUser().getEmail() : "Guest";

            responses.add(AdminTopLinkResponse.builder()
                    .rank(rank++)
                    .id(url.getId())
                    .shortCode(url.getShortCode())
                    .originalUrl(url.getOriginalUrl())
                    .clicks(url.getClickCount() != null ? url.getClickCount() : 0L)
                    .owner(owner)
                    .createdAt(url.getCreatedAt())
                    .status(isActive ? "Active" : "Expired")
                    .build());
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTopUserResponse> getTopUsers(int limit) {
        int max = limit > 0 ? limit : 10;
        List<User> users = userRepository.findAll();

        List<AdminTopUserResponse> userStats = new ArrayList<>();
        for (User user : users) {
            List<Url> userUrls = urlRepository.findAllByUser(user);
            long totalClicks = userUrls.stream()
                    .mapToLong(u -> (u.getClickCount() != null ? u.getClickCount() : 0L))
                    .sum();

            userStats.add(AdminTopUserResponse.builder()
                    .userId(user.getId())
                    .name(user.getName() != null ? user.getName() : "User")
                    .email(user.getEmail())
                    .links(userUrls.size())
                    .totalClicks(totalClicks)
                    .build());
        }

        userStats.sort(Comparator.comparingLong(AdminTopUserResponse::getTotalClicks).reversed());

        List<AdminTopUserResponse> topUsers = new ArrayList<>();
        int rank = 1;
        for (AdminTopUserResponse stat : userStats) {
            if (topUsers.size() >= max) break;
            stat.setRank(rank++);
            topUsers.add(stat);
        }
        return topUsers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionItem> getPlatformGeography(String range) {
        LocalDateTime since = parseSinceDate(range);
        long totalClicks = clickEventRepository.countPlatformClicksAfter(since);
        return mapToDistributionItems(clickEventRepository.getPlatformCountryDistribution(since), totalClicks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionItem> getPlatformDevices(String range) {
        LocalDateTime since = parseSinceDate(range);
        long totalClicks = clickEventRepository.countPlatformClicksAfter(since);
        return mapToDistributionItems(clickEventRepository.getPlatformDeviceDistribution(since), totalClicks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRecentActivityResponse> getRecentActivity() {
        List<AdminRecentActivityResponse> activityList = new ArrayList<>();

        // Recent user registrations
        List<User> recentUsers = userRepository.findTop10ByOrderByCreatedAtDesc();
        for (User user : recentUsers) {
            if (user.getCreatedAt() != null) {
                activityList.add(AdminRecentActivityResponse.builder()
                        .id("user-" + user.getId())
                        .type("USER_REGISTERED")
                        .title("New user registered")
                        .description(user.getEmail() + " created an account.")
                        .timestamp(LocalDateTime.ofInstant(user.getCreatedAt(), ZoneOffset.UTC))
                        .build());
            }
        }

        // Recent URL creations
        List<Url> recentUrls = urlRepository.findTop10ByOrderByCreatedAtDesc();
        for (Url url : recentUrls) {
            if (url.getCreatedAt() != null) {
                String owner = url.getUser() != null ? url.getUser().getEmail() : "Guest";
                activityList.add(AdminRecentActivityResponse.builder()
                        .id("url-" + url.getId())
                        .type("URL_CREATED")
                        .title("New short link created")
                        .description("/" + url.getShortCode() + " created by " + owner)
                        .timestamp(url.getCreatedAt())
                        .build());
            }
        }

        // Sort by timestamp descending
        activityList.sort(Comparator.comparing(AdminRecentActivityResponse::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        return activityList.size() > 10 ? activityList.subList(0, 10) : activityList;
    }

    // --- Helpers ---

    private LocalDateTime parseSinceDate(String range) {
        if (range == null || range.isBlank()) {
            return LocalDateTime.now().minusDays(30);
        }
        return switch (range.toLowerCase()) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            case "90d" -> LocalDateTime.now().minusDays(90);
            case "1y" -> LocalDateTime.now().minusDays(365);
            case "all" -> LocalDateTime.of(2020, 1, 1, 0, 0);
            default -> LocalDateTime.now().minusDays(30);
        };
    }

    private List<AdminGrowthPoint> mapToContinuousAdminTimeSeries(List<Object[]> rows, LocalDateTime since, LocalDate endDate) {
        Map<String, Long> countMap = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    countMap.put(row[0].toString().trim(), ((Number) row[1]).longValue());
                }
            }
        }

        List<AdminGrowthPoint> points = new ArrayList<>();
        LocalDate current = since.toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (!current.isAfter(endDate)) {
            String dateKey = current.format(formatter);
            points.add(new AdminGrowthPoint(dateKey, countMap.getOrDefault(dateKey, 0L)));
            current = current.plusDays(1);
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

    private double roundToOneDecimal(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return 0.0;
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
