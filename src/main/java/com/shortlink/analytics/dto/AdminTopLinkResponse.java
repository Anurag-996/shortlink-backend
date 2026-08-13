package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Leaderboard item for highest-performing links across the platform for ADMIN.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTopLinkResponse {
    private int rank;
    private Long id;
    private String shortCode;
    private String originalUrl;
    private long clicks;
    private String owner; // Owner email or "Guest"
    private LocalDateTime createdAt;
    private String status;
}
