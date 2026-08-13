package com.shortlink.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shortlink.dto.response.ApiResponse;
import com.shortlink.dto.response.PageResponse;
import com.shortlink.user.dto.AdminUserResponse;
import com.shortlink.user.dto.AdminUserStatsResponse;
import com.shortlink.user.dto.UpdateUserRoleRequest;
import com.shortlink.user.dto.UpdateUserStatusRequest;
import com.shortlink.user.service.AdminUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// REST controller for Admin User Management operations, protected with ADMIN role.
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // GET /api/admin/users - Retrieves paginated and searchable users list
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<AdminUserResponse> response = adminUserService.getUsers(page, size, search, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    // GET /api/admin/users/stats - Retrieves summary statistics of all users
    @GetMapping("/stats")
    public ResponseEntity<AdminUserStatsResponse> getUserStats() {
        AdminUserStatsResponse response = adminUserService.getUserStats();
        return ResponseEntity.ok(response);
    }

    // PATCH /api/admin/users/{id}/status - Enables or disables a user account
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        AdminUserResponse response = adminUserService.updateUserStatus(id, request.enabled());
        return ResponseEntity.ok(response);
    }

    // PATCH /api/admin/users/{id}/role - Updates user role (USER or ADMIN)
    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        AdminUserResponse response = adminUserService.updateUserRole(id, request.role());
        return ResponseEntity.ok(response);
    }

    // DELETE /api/admin/users/{id} - Permanently deletes user and cascades resources
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User account and all associated data successfully deleted"));
    }
}
