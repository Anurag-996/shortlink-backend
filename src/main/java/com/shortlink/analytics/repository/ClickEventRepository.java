package com.shortlink.analytics.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shortlink.analytics.entity.ClickEvent;
import com.shortlink.entity.Url;

// Repository for ClickEvent entity with database-level aggregation queries.
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    // --- Basic Counts by ShortUrl ---
    long countByShortUrl(Url shortUrl);

    long countByShortUrlAndClickedAtAfter(Url shortUrl, LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT c.visitorHash) FROM ClickEvent c WHERE c.shortUrl = :shortUrl")
    long countUniqueVisitorsByShortUrl(@Param("shortUrl") Url shortUrl);

    @Query("SELECT COUNT(DISTINCT c.visitorHash) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since")
    long countUniqueVisitorsByShortUrlAndClickedAtAfter(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    // --- Basic Counts by Multiple ShortUrls (User dashboard) ---
    long countByShortUrlIn(List<Url> shortUrls);

    long countByShortUrlInAndClickedAtAfter(List<Url> shortUrls, LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT c.visitorHash) FROM ClickEvent c WHERE c.shortUrl IN :shortUrls")
    long countUniqueVisitorsByShortUrlIn(@Param("shortUrls") List<Url> shortUrls);

    @Query("SELECT COUNT(DISTINCT c.visitorHash) FROM ClickEvent c WHERE c.shortUrl IN :shortUrls AND c.clickedAt >= :since")
    long countUniqueVisitorsByShortUrlInAndClickedAtAfter(@Param("shortUrls") List<Url> shortUrls, @Param("since") LocalDateTime since);

    // --- Link-Level Distributions ---
    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> getDeviceDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT c.browser, COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY c.browser ORDER BY COUNT(c) DESC")
    List<Object[]> getBrowserDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT c.operatingSystem, COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY c.operatingSystem ORDER BY COUNT(c) DESC")
    List<Object[]> getOsDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT c.refererSource, COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY c.refererSource ORDER BY COUNT(c) DESC")
    List<Object[]> getRefererDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT c.country, COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> getCountryDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT c.city, COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since AND c.city <> 'UNKNOWN' GROUP BY c.city ORDER BY COUNT(c) DESC")
    List<Object[]> getCityDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    // --- Time Series Aggregations by Link ---
    @Query("SELECT FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD'), COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD') ORDER BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD') ASC")
    List<Object[]> getTimeSeriesDailyByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD HH24:00'), COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD HH24:00') ORDER BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD HH24:00') ASC")
    List<Object[]> getTimeSeriesHourlyByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT FUNCTION('to_char', c.clickedAt, 'HH24'), COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY FUNCTION('to_char', c.clickedAt, 'HH24') ORDER BY FUNCTION('to_char', c.clickedAt, 'HH24') ASC")
    List<Object[]> getHourlyDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    @Query("SELECT FUNCTION('to_char', c.clickedAt, 'FMDay'), COUNT(c) FROM ClickEvent c WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since GROUP BY FUNCTION('to_char', c.clickedAt, 'FMDay')")
    List<Object[]> getDayOfWeekDistributionByShortUrl(@Param("shortUrl") Url shortUrl, @Param("since") LocalDateTime since);

    // --- User Dashboard Aggregations ---
    @Query("SELECT FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD'), COUNT(c) FROM ClickEvent c WHERE c.shortUrl IN :shortUrls AND c.clickedAt >= :since GROUP BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD') ORDER BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD') ASC")
    List<Object[]> getTimeSeriesDailyByShortUrlIn(@Param("shortUrls") List<Url> shortUrls, @Param("since") LocalDateTime since);

    @Query("SELECT c.refererSource, COUNT(c) FROM ClickEvent c WHERE c.shortUrl IN :shortUrls AND c.clickedAt >= :since GROUP BY c.refererSource ORDER BY COUNT(c) DESC")
    List<Object[]> getRefererDistributionByShortUrlIn(@Param("shortUrls") List<Url> shortUrls, @Param("since") LocalDateTime since);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c WHERE c.shortUrl IN :shortUrls AND c.clickedAt >= :since GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> getDeviceDistributionByShortUrlIn(@Param("shortUrls") List<Url> shortUrls, @Param("since") LocalDateTime since);

    // --- Platform Admin Aggregations ---
    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.clickedAt >= :since")
    long countPlatformClicksAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.clickedAt >= :start AND c.clickedAt < :end")
    long countPlatformClicksBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD'), COUNT(c) FROM ClickEvent c WHERE c.clickedAt >= :since GROUP BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD') ORDER BY FUNCTION('to_char', c.clickedAt, 'YYYY-MM-DD') ASC")
    List<Object[]> getPlatformTimeSeriesDaily(@Param("since") LocalDateTime since);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c WHERE c.clickedAt >= :since GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> getPlatformDeviceDistribution(@Param("since") LocalDateTime since);

    @Query("SELECT c.country, COUNT(c) FROM ClickEvent c WHERE c.clickedAt >= :since GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> getPlatformCountryDistribution(@Param("since") LocalDateTime since);

    @Query("SELECT c.city, COUNT(c) FROM ClickEvent c WHERE c.clickedAt >= :since AND c.city <> 'UNKNOWN' GROUP BY c.city ORDER BY COUNT(c) DESC")
    List<Object[]> getPlatformCityDistribution(@Param("since") LocalDateTime since);

    @Query("SELECT c.shortUrl.id, COUNT(c) as total FROM ClickEvent c GROUP BY c.shortUrl.id ORDER BY total DESC")
    List<Object[]> getTopUrlIds(Pageable pageable);

    // --- Cascading Cleanup ---
    void deleteByShortUrl(Url shortUrl);

    void deleteByShortUrlIn(List<Url> shortUrls);
}
