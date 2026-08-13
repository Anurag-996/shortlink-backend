package com.shortlink.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shortlink.dto.request.LoginRequest;
import com.shortlink.entity.PendingRegistration;
import com.shortlink.entity.RefreshToken;
import com.shortlink.exception.AccountDeletionPendingException;
import com.shortlink.exception.AccountDisabledException;
import com.shortlink.exception.BadRequestException;
import com.shortlink.exception.InvalidCredentialsException;
import com.shortlink.exception.InvalidTokenException;
import com.shortlink.repository.PendingRegistrationRepository;
import com.shortlink.repository.RefreshTokenRepository;
import com.shortlink.security.jwt.JwtService;
import com.shortlink.service.AuthService;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Implementation of AuthService managing credentials validation, JWT issuance, and refresh token rotation.
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneId.of("UTC"));

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthSessionResult login(LoginRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    log.warn("Login failed: email [{}] not found", normalizedEmail);

                    PendingRegistration pending = pendingRegistrationRepository
                            .findByEmail(normalizedEmail)
                            .orElse(null);

                    if (pending != null) {
                        log.warn(
                                "Found pending unverified registration for email: {}",
                                normalizedEmail
                        );
                        throw new AccountDisabledException(
                                "Please verify your email address before logging in"
                        );
                    }

                    throw new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            log.warn(
                    "Invalid password attempt for email: {}",
                    normalizedEmail
            );

            throw new InvalidCredentialsException();
        }

        if (user.isDeletionPending()) {

            log.warn(
                    "Login attempt for account pending deletion: {}",
                    normalizedEmail
            );

            String dateMsg = "";
            if (user.getDeletionScheduledAt() != null) {
                dateMsg = " on " + DATE_FORMATTER.format(user.getDeletionScheduledAt());
            }

            throw new AccountDeletionPendingException(
                    "Your account is scheduled for permanent deletion" + dateMsg
            );
        }

        if (!user.isEnabled()) {

            log.warn(
                    "Login attempt for disabled account: {}",
                    normalizedEmail
            );

            throw new AccountDisabledException(
                    "Please verify your email address before logging in"
            );
        }

        // Multi-session policy: Issue independent rotating refresh token for this device/session.
        String accessToken
                = jwtService.generateAccessToken(user);

        String refreshTokenString
                = jwtService.generateRefreshTokenString();

        long refreshExpirationMillis
                = jwtService.getRefreshTokenExpirationMillis();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(
                        Instant.now().plusMillis(refreshExpirationMillis)
                )
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info(
                "User [{}] successfully authenticated",
                normalizedEmail
        );

        return new AuthSessionResult(
                accessToken,
                jwtService.getAccessTokenExpirationMillis(),
                refreshTokenString,
                refreshExpirationMillis
        );
    }

    @Override
    @Transactional
    public AuthSessionResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidTokenException();
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(InvalidTokenException::new);

        if (!refreshToken.isValid()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException();
        }

        User user = refreshToken.getUser();
        if (user.isDeletionPending()) {
            refreshTokenRepository.delete(refreshToken);
            String dateMsg = "";
            if (user.getDeletionScheduledAt() != null) {
                dateMsg = " on " + DATE_FORMATTER.format(user.getDeletionScheduledAt());
            }
            throw new AccountDeletionPendingException("Your account is scheduled for permanent deletion" + dateMsg);
        }

        if (!user.isEnabled()) {
            refreshTokenRepository.delete(refreshToken);
            throw new AccountDisabledException("Please verify your email address before logging in");
        }

        // Option A (Fixed Refresh Token): Issue new accessToken while keeping existing valid refreshToken
        String newAccessToken = jwtService.generateAccessToken(user);
        long refreshExpirationMillis = jwtService.getRefreshTokenExpirationMillis();

        log.info("Successfully refreshed access token for user [{}]", user.getEmail());

        return new AuthSessionResult(
                newAccessToken,
                jwtService.getAccessTokenExpirationMillis(),
                refreshTokenValue,
                refreshExpirationMillis
        );
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(refreshTokenRepository::delete);
            log.info("Current device refresh session successfully revoked");
        }
    }

    @Override
    @Transactional
    public void logoutAll(String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            userRepository.findByEmail(userEmail.trim().toLowerCase())
                    .ifPresent(user -> {
                        refreshTokenRepository.deleteByUser(user);
                        log.info("All refresh sessions successfully revoked for user [{}]", user.getEmail());
                    });
        }
    }

    @Override
    @Transactional
    public AuthSessionResult cancelAccountDeletion(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Invalid password attempt for cancel deletion on email: {}", normalizedEmail);
            throw new InvalidCredentialsException();
        }

        if (!user.isDeletionPending()) {
            throw new BadRequestException("Account is not scheduled for deletion");
        }

        // Restore user account and cancel deletion
        user.setDeletionPending(false);
        user.setDeletionRequestedAt(null);
        user.setDeletionScheduledAt(null);
        user.setEnabled(true);

        userRepository.save(user);

        log.info("Cancelled scheduled deletion and restored user account [{}]", normalizedEmail);

        // Issue new authentication tokens for immediate login
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshTokenString();
        long refreshExpirationMillis = jwtService.getRefreshTokenExpirationMillis();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMillis))
                .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthSessionResult(
                accessToken,
                jwtService.getAccessTokenExpirationMillis(),
                refreshTokenString,
                refreshExpirationMillis
        );
    }
}
