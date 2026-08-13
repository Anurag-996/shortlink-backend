package com.shortlink.service;

import com.shortlink.analytics.repository.ClickEventRepository;
import com.shortlink.dto.response.ApiResponse;
import com.shortlink.entity.Url;
import com.shortlink.exception.BadRequestException;
import com.shortlink.repository.PasswordResetTokenRepository;
import com.shortlink.repository.RefreshTokenRepository;
import com.shortlink.repository.UrlRepository;
import com.shortlink.user.Role;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Service handling 7-day scheduled account deletion workflows for USER accounts.
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RedisCacheService redisCacheService;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneId.of("UTC"));

    @Transactional
    public ApiResponse requestAccountDeletion(User user) {
        if (user == null) {
            throw new BadRequestException("User not authenticated");
        }

        // ADMIN accounts cannot be deleted
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be deleted.");
        }

        if (user.isDeletionPending()) {
            String formattedScheduledDate = DATE_FORMATTER.format(user.getDeletionScheduledAt());
            throw new BadRequestException("Account deletion is already scheduled for " + formattedScheduledDate);
        }

        Instant now = Instant.now();
        Instant scheduledAt = now.plus(7, ChronoUnit.DAYS);

        user.setDeletionPending(true);
        user.setDeletionRequestedAt(now);
        user.setDeletionScheduledAt(scheduledAt);
        user.setEnabled(false);

        userRepository.save(user);

        // Invalidate all active sessions immediately
        refreshTokenRepository.deleteByUser(user);

        String formattedDate = DATE_FORMATTER.format(scheduledAt);
        String displayName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "User";

        try {
            emailService.sendAccountDeletionEmail(user.getEmail(), displayName, formattedDate);
        } catch (Exception e) {
            log.error("Failed to send account deletion email to {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("Account deletion requested for user [{}] - scheduled at {}", user.getEmail(), scheduledAt);
        return ApiResponse.ok("Account deletion successfully requested. Your account is disabled and will be permanently deleted on " + formattedDate);
    }

    // Runs once every hour to process users whose 7-day deletion grace period has expired.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processScheduledDeletions() {
        Instant now = Instant.now();
        List<User> expiredUsers = userRepository.findAllByDeletionPendingTrueAndDeletionScheduledAtBefore(now);

        if (expiredUsers.isEmpty()) {
            return;
        }

        log.info("Found {} user accounts due for permanent deletion", expiredUsers.size());

        for (User user : expiredUsers) {
            try {
                // Find all links created by the user
                List<Url> urls = urlRepository.findAllByUser(user);

                // Evict short codes from Redis cache
                for (Url url : urls) {
                    redisCacheService.evictUrl(url.getShortCode());
                }

                // Delete related click / analytics data
                if (!urls.isEmpty()) {
                    clickEventRepository.deleteByShortUrlIn(urls);
                }

                // Delete all user links
                urlRepository.deleteByUser(user);

                // Clean up tokens
                refreshTokenRepository.deleteByUser(user);
                passwordResetTokenRepository.deleteByUser(user);

                // Delete user entity permanently
                userRepository.delete(user);

                log.info("Permanently purged user [{}] with {} short links and analytics records", user.getEmail(), urls.size());
            } catch (Exception e) {
                log.error("Error processing permanent deletion for user [{}]: {}", user.getEmail(), e.getMessage(), e);
            }
        }
    }
}
