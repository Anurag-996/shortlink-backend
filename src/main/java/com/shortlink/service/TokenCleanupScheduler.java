package com.shortlink.service;

import com.shortlink.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Scheduled background component responsible for purging expired tokens and soft-rotated tokens past their grace period.
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    // Runs once every hour at minute 0 to keep the refresh_tokens database table clean and performant.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupTokens() {
        try {
            Instant now = Instant.now();
            Instant graceCutoff = now.minus(5, ChronoUnit.MINUTES);
            int deletedCount = refreshTokenRepository.deleteExpiredAndRotatedTokens(now, graceCutoff);
            if (deletedCount > 0) {
                log.info("TokenCleanupScheduler: Purged {} expired/rotated refresh tokens from database", deletedCount);
            }
        } catch (Exception e) {
            log.error("TokenCleanupScheduler: Error during background token cleanup: {}", e.getMessage(), e);
        }
    }
}
