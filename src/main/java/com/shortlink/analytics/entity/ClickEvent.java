package com.shortlink.analytics.entity;

import com.shortlink.entity.Url;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Raw click event entity capturing redirection analytics.
@Entity
@Table(
    name = "click_events",
    indexes = {
        @Index(name = "idx_click_events_short_url_id", columnList = "short_url_id"),
        @Index(name = "idx_click_events_short_code", columnList = "short_code"),
        @Index(name = "idx_click_events_clicked_at", columnList = "clicked_at"),
        @Index(name = "idx_click_events_url_clicked", columnList = "short_url_id, clicked_at"),
        @Index(name = "idx_click_events_visitor_hash", columnList = "visitor_hash")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_url_id", nullable = false)
    private Url shortUrl;

    @Column(name = "short_code", nullable = false, length = 32)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    // Privacy-conscious anonymized hash (SHA-256) of IP + User-Agent for unique visitor calculations
    @Column(name = "visitor_hash", length = 64)
    private String visitorHash;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(name = "referer", length = 2048)
    private String referer;

    // Derived friendly referrer source (e.g. Google, Instagram, Direct, etc.)
    @Column(name = "referer_source", length = 64)
    private String refererSource;

    @Column(name = "country", length = 64)
    @Builder.Default
    private String country = "UNKNOWN";

    @Column(name = "region", length = 64)
    @Builder.Default
    private String region = "UNKNOWN";

    @Column(name = "city", length = 64)
    @Builder.Default
    private String city = "UNKNOWN";

    // Classified device: MOBILE, DESKTOP, TABLET, UNKNOWN
    @Column(name = "device_type", length = 32)
    @Builder.Default
    private String deviceType = "UNKNOWN";

    // Classified browser: Chrome, Safari, Edge, Firefox, Opera, Other, Unknown
    @Column(name = "browser", length = 32)
    @Builder.Default
    private String browser = "Unknown";

    // Classified OS: Android, iOS, Windows, macOS, Linux, Other, Unknown
    @Column(name = "operating_system", length = 32)
    @Builder.Default
    private String operatingSystem = "Unknown";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
