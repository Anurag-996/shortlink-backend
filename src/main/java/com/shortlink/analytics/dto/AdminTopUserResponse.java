package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Leaderboard item for users generating the most platform activity.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTopUserResponse {
    private int rank;
    private Long userId;
    private String name;
    private String email;
    private long links;
    private long totalClicks;
}
