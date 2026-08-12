package com.shortlink.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// Entity representing a single URL click analytics event.
@Entity
@Table(
    name = "url_clicks",
    indexes = {
        @Index(name = "idx_url_clicks_short_code", columnList = "short_code"),
        @Index(name = "idx_url_clicks_clicked_at", columnList = "clicked_at"),
        @Index(name = "idx_url_clicks_event_id", columnList = "event_id", unique = true)
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "short_code", nullable = false)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(name = "referrer", length = 2048)
    private String referrer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
