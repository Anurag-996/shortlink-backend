package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Event item representing meaningful recent platform activity.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecentActivityResponse {
    private String id;
    private String type; // USER_REGISTERED, URL_CREATED, URL_MILESTONE, USER_VERIFIED, ACCOUNT_DELETION
    private String title;
    private String description;
    private LocalDateTime timestamp;
}
