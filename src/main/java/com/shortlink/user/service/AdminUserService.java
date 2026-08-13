package com.shortlink.user.service;

import com.shortlink.dto.response.PageResponse;
import com.shortlink.user.Role;
import com.shortlink.user.dto.AdminUserResponse;
import com.shortlink.user.dto.AdminUserStatsResponse;

// Service interface for Admin user management operations.
public interface AdminUserService {

    // Retrieves paginated and searchable users list with link stats.
    PageResponse<AdminUserResponse> getUsers(int page, int size, String search, String sortBy, String direction);

    // Retrieves platform-wide user statistics.
    AdminUserStatsResponse getUserStats();

    // Enables or disables a user account.
    AdminUserResponse updateUserStatus(Long userId, boolean enabled);

    // Updates a user's role (USER or ADMIN).
    AdminUserResponse updateUserRole(Long userId, Role role);

    // Permanently deletes a user and cascades their URLs and tokens.
    void deleteUser(Long userId);
}
