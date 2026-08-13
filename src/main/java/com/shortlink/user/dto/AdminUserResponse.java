package com.shortlink.user.dto;

import java.time.Instant;

import com.shortlink.user.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Response DTO containing detailed user profile, role, status, and link statistics for Admin management.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean deletionPending;
    private Instant createdAt;
    private long totalLinks;
    private long totalClicks;
}
