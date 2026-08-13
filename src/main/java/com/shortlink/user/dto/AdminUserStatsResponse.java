package com.shortlink.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Summary metrics response for Admin User Management.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long disabledUsers;
    private long adminUsers;
    private long deletionPendingUsers;
}
