package com.shortlink.service.impl;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shortlink.dto.request.LoginRequest;
import com.shortlink.entity.PendingRegistration;
import com.shortlink.entity.RefreshToken;
import com.shortlink.exception.AccountDisabledException;
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

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthSessionResult login(LoginRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {

            PendingRegistration pending
                    = pendingRegistrationRepository
                            .findByEmail(normalizedEmail)
                            .orElse(null);

            if (pending != null
                    && passwordEncoder.matches(
                            request.password(),
                            pending.getPasswordHash())) {

                log.warn(
                        "Login attempt for pending unverified email: {}",
                        normalizedEmail
                );

                throw new AccountDisabledException(
                        "Please verify your email address before logging in"
                );
            }

            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            log.warn(
                    "Invalid password attempt for email: {}",
                    normalizedEmail
            );

            throw new InvalidCredentialsException();
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

        if (user.isDeletionPending()) {

            log.warn(
                    "Login attempt for account pending deletion: {}",
                    normalizedEmail
            );

            throw new AccountDisabledException(
                    "Your account is pending deletion"
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
                .revoked(false)
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
        if (!user.isEnabled()) {
            refreshTokenRepository.delete(refreshToken);
            throw new AccountDisabledException();
        }

        // Rotate token: delete current token and generate new one
        refreshTokenRepository.delete(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenString = jwtService.generateRefreshTokenString();
        long refreshExpirationMillis = jwtService.getRefreshTokenExpirationMillis();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenString)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMillis))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);
        log.info("Successfully refreshed session for user [{}]", user.getEmail());

        return new AuthSessionResult(
                newAccessToken,
                jwtService.getAccessTokenExpirationMillis(),
                newRefreshTokenString,
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
}
