package com.shortlink.service;

import com.shortlink.repository.AccountActivationTokenRepository;
import com.shortlink.repository.EmailVerificationAttemptRepository;
import com.shortlink.repository.PasswordResetTokenRepository;
import com.shortlink.repository.PendingRegistrationRepository;
import com.shortlink.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

// Scheduled service to clean up expired registration requests, stale tokens, and rate-limit logs.
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledCleanupService {

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationAttemptRepository attemptRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // Runs every 6 hours to clean up expired pending registrations, password reset tokens, and refresh tokens
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Running scheduled cleanup for expired pending registrations and tokens at {}", now);

        try {
            pendingRegistrationRepository.deleteByExpiresAtBefore(now);
            accountActivationTokenRepository.deleteByExpiresAtBefore(now);
            passwordResetTokenRepository.deleteByExpiresAtBefore(now);
            attemptRepository.deleteBySentAtBefore(now.minusDays(7));
            refreshTokenRepository.deleteExpiredTokens(Instant.now());
        } catch (Exception e) {
            log.error("Failed to run scheduled token cleanup: {}", e.getMessage(), e);
        }
    }
}
