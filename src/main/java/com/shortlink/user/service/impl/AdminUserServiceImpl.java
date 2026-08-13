package com.shortlink.user.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shortlink.analytics.repository.ClickEventRepository;
import com.shortlink.dto.response.PageResponse;
import com.shortlink.entity.Url;
import com.shortlink.exception.BadRequestException;
import com.shortlink.repository.RefreshTokenRepository;
import com.shortlink.repository.UrlRepository;
import com.shortlink.security.util.SecurityUtils;
import com.shortlink.service.RedisCacheService;
import com.shortlink.user.Role;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;
import com.shortlink.user.dto.AdminUserResponse;
import com.shortlink.user.dto.AdminUserStatsResponse;
import com.shortlink.user.service.AdminUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Implementation of AdminUserService managing administrative user lifecycle, status, roles, and cascade deletions.
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ClickEventRepository clickEventRepository;
    private final RedisCacheService redisCacheService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(int page, int size, String search, String sortBy, String direction) {
        int validPage = Math.max(0, page);
        int validSize = size > 0 ? Math.min(size, 100) : 10;

        String property = "createdAt";
        if (sortBy != null && !sortBy.isBlank()) {
            String clean = sortBy.trim().toLowerCase();
            switch (clean) {
                case "name" -> property = "name";
                case "email" -> property = "email";
                case "role" -> property = "role";
                case "enabled", "status" -> property = "enabled";
                case "createdat", "created" -> property = "createdAt";
                default -> property = "createdAt";
            }
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(sortDirection, property));

        Page<User> userPage;
        if (search != null && !search.trim().isBlank()) {
            userPage = userRepository.searchUsers(search.trim(), pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<AdminUserResponse> content = userPage.getContent().stream()
                .map(this::mapToAdminUserResponse)
                .toList();

        return PageResponse.of(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserStatsResponse getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long disabledUsers = userRepository.countByEnabledFalse();
        long adminUsers = userRepository.countByRole(Role.ADMIN);
        long deletionPending = userRepository.countByDeletionPendingTrue();

        return AdminUserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)
                .adminUsers(adminUsers)
                .deletionPendingUsers(deletionPending)
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, boolean enabled) {
        User targetUser = findUserById(userId);
        User currentUser = SecurityUtils.getCurrentUser();

        if (targetUser.getId().equals(currentUser.getId()) && !enabled) {
            throw new BadRequestException("You cannot disable your own admin account");
        }

        targetUser.setEnabled(enabled);
        User savedUser = userRepository.save(targetUser);

        // If disabling, revoke all active refresh tokens immediately
        if (!enabled) {
            refreshTokenRepository.deleteByUser(savedUser);
            log.info("Revoked all active sessions for disabled user [{}]", savedUser.getEmail());
        }

        log.info("Admin [{}] updated user [{}] enabled status to: {}", currentUser.getEmail(), savedUser.getEmail(), enabled);
        return mapToAdminUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRole(Long userId, Role role) {
        User targetUser = findUserById(userId);
        User currentUser = SecurityUtils.getCurrentUser();

        if (targetUser.getId().equals(currentUser.getId()) && role != Role.ADMIN) {
            long totalAdmins = userRepository.countByRole(Role.ADMIN);
            if (totalAdmins <= 1) {
                throw new BadRequestException("Cannot remove admin privileges from the sole remaining administrator");
            }
        }

        targetUser.setRole(role);
        User savedUser = userRepository.save(targetUser);

        log.info("Admin [{}] changed role of user [{}] to: {}", currentUser.getEmail(), savedUser.getEmail(), role);
        return mapToAdminUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User targetUser = findUserById(userId);
        User currentUser = SecurityUtils.getCurrentUser();

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot delete your own admin account from user management");
        }

        List<Url> userUrls = urlRepository.findAllByUser(targetUser);

        // 1. Evict short codes from Redis cache
        for (Url url : userUrls) {
            redisCacheService.evictUrl(url.getShortCode());
        }

        // 2. Cascade delete click analytics events
        if (!userUrls.isEmpty()) {
            clickEventRepository.deleteByShortUrlIn(userUrls);
        }

        // 3. Delete user URLs
        urlRepository.deleteByUser(targetUser);

        // 4. Delete refresh tokens
        refreshTokenRepository.deleteByUser(targetUser);

        // 5. Delete user account
        userRepository.delete(targetUser);

        log.info("Admin [{}] permanently deleted user [{}] and all associated resources", currentUser.getEmail(), targetUser.getEmail());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found with ID: " + userId));
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        long totalLinks = urlRepository.countByUser(user);
        long totalClicks = urlRepository.sumClicksByUser(user);

        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .deletionPending(user.isDeletionPending())
                .createdAt(user.getCreatedAt())
                .totalLinks(totalLinks)
                .totalClicks(totalClicks)
                .build();
    }
}
