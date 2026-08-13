package com.shortlink.service;

import com.shortlink.dto.request.RegisterRequest;
import com.shortlink.dto.response.ApiResponse;
import com.shortlink.entity.AccountActivationToken;
import com.shortlink.entity.EmailVerificationAttempt;
import com.shortlink.entity.PendingRegistration;
import com.shortlink.exception.BadRequestException;
import com.shortlink.exception.DuplicateResourceException;
import com.shortlink.repository.AccountActivationTokenRepository;
import com.shortlink.repository.EmailVerificationAttemptRepository;
import com.shortlink.repository.PendingRegistrationRepository;
import com.shortlink.user.Role;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;
import com.shortlink.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// Service managing user registration initiation, email verification token validation, and account activation.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final EmailVerificationAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.auth.verification-token-expiration-hours:24}")
    private int verificationTokenExpirationHours;

    @Transactional
    public ApiResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        // Check if an active user already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already registered. Please login.");
        }

        validateRateLimit(normalizedEmail);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = TokenHashUtil.hashToken(rawToken);
        String passwordHash = passwordEncoder.encode(request.password());
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(verificationTokenExpirationHours);

        PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> PendingRegistration.builder()
                        .email(normalizedEmail)
                        .build());

        pendingRegistration.setName(request.name().trim());
        pendingRegistration.setPasswordHash(passwordHash);
        pendingRegistration.setVerificationTokenHash(tokenHash);
        pendingRegistration.setExpiresAt(expiresAt);

        pendingRegistrationRepository.save(pendingRegistration);

        String verificationLink = frontendUrl + "/verify-email?token=" + rawToken;
        emailService.sendVerificationEmail(normalizedEmail, request.name().trim(), verificationLink);

        attemptRepository.save(
                EmailVerificationAttempt.builder()
                        .email(normalizedEmail)
                        .sentAt(LocalDateTime.now())
                        .build()
        );

        log.info("Pending registration created for email: {}", normalizedEmail);
        return ApiResponse.ok("Registration initiated successfully. Please check your email to verify your account.");
    }

    @Transactional
    public ApiResponse verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Invalid verification token");
        }

        String tokenHash = TokenHashUtil.hashToken(rawToken.trim());

        // Case 1: Pending registration for new signup
        Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByVerificationTokenHash(tokenHash);
        if (pendingOpt.isPresent()) {
            PendingRegistration pending = pendingOpt.get();

            if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
                pendingRegistrationRepository.delete(pending);
                throw new BadRequestException("Verification token expired. Please register again.");
            }

            if (userRepository.existsByEmail(pending.getEmail())) {
                pendingRegistrationRepository.delete(pending);
                throw new DuplicateResourceException("Email already registered. Please login.");
            }

            // Create the actual User account with default role USER
            User user = User.builder()
                    .name(pending.getName())
                    .email(pending.getEmail())
                    .password(pending.getPasswordHash())
                    .role(Role.USER)
                    .enabled(true)
                    .deletionPending(false)
                    .build();

            User savedUser = userRepository.save(user);

            // Invalidate/delete pending registration
            pendingRegistrationRepository.delete(pending);

            // Send welcome email after account creation
            try {
                emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());
            } catch (Exception e) {
                log.error("Failed to send welcome email to {}: {}", savedUser.getEmail(), e.getMessage());
            }

            log.info("User [{}] successfully verified and created with role USER", savedUser.getEmail());
            return ApiResponse.ok("Email verified successfully! You can now log in.");
        }

        // Case 2: Existing disabled user activation token
        Optional<AccountActivationToken> activationOpt = accountActivationTokenRepository.findByTokenHash(tokenHash);
        if (activationOpt.isPresent()) {
            AccountActivationToken activationToken = activationOpt.get();

            if (activationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                accountActivationTokenRepository.delete(activationToken);
                throw new BadRequestException("Verification token expired. Please request a new one.");
            }

            User user = activationToken.getUser();
            user.setEnabled(true);
            userRepository.save(user);

            accountActivationTokenRepository.delete(activationToken);

            log.info("Existing user [{}] successfully activated and enabled", user.getEmail());
            return ApiResponse.ok("Email verified successfully! You can now log in.");
        }

        throw new BadRequestException("Invalid or expired verification token");
    }

    @Transactional
    public ApiResponse resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email cannot be blank");
        }

        String normalizedEmail = email.trim().toLowerCase();

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(normalizedEmail)
                .orElse(null);

        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);

        // Case 1: Pending registration for new unverified user
        if (pending != null && existingUser == null) {
            validateRateLimit(normalizedEmail);

            String rawToken = UUID.randomUUID().toString();
            String tokenHash = TokenHashUtil.hashToken(rawToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(verificationTokenExpirationHours);

            pending.setVerificationTokenHash(tokenHash);
            pending.setExpiresAt(expiresAt);
            pendingRegistrationRepository.save(pending);

            String verificationLink = frontendUrl + "/verify-email?token=" + rawToken;
            emailService.sendVerificationEmail(pending.getEmail(), pending.getName(), verificationLink);

            attemptRepository.save(
                    EmailVerificationAttempt.builder()
                            .email(normalizedEmail)
                            .sentAt(LocalDateTime.now())
                            .build()
            );

            log.info("Resent verification email for pending registration: {}", normalizedEmail);
        }
        // Case 2: Existing user whose enabled status is false
        else if (existingUser != null && !existingUser.isEnabled() && !existingUser.isDeletionPending()) {
            validateRateLimit(normalizedEmail);

            String rawToken = UUID.randomUUID().toString();
            String tokenHash = TokenHashUtil.hashToken(rawToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(verificationTokenExpirationHours);

            AccountActivationToken activationToken = accountActivationTokenRepository.findByUser(existingUser)
                    .orElseGet(() -> AccountActivationToken.builder()
                            .user(existingUser)
                            .build());

            activationToken.setTokenHash(tokenHash);
            activationToken.setExpiresAt(expiresAt);
            accountActivationTokenRepository.save(activationToken);

            String verificationLink = frontendUrl + "/verify-email?token=" + rawToken;
            String displayName = (existingUser.getName() != null && !existingUser.getName().isBlank())
                    ? existingUser.getName() : "User";
            emailService.sendAccountActivationEmail(existingUser.getEmail(), displayName, verificationLink);

            attemptRepository.save(
                    EmailVerificationAttempt.builder()
                            .email(normalizedEmail)
                            .sentAt(LocalDateTime.now())
                            .build()
            );

            log.info("Dispatched account activation verification email for existing disabled user: {}", normalizedEmail);
        }

        // Non-enumerating response
        return ApiResponse.ok("If a pending registration exists for this email, a verification link has been sent.");
    }

    private void validateRateLimit(String email) {
        long hourlyCount = attemptRepository.countByEmailAndSentAtAfter(
                email,
                LocalDateTime.now().minusHours(1)
        );

        if (hourlyCount >= 5) {
            throw new BadRequestException("Verification email limit exceeded. Please try again later.");
        }

        long dailyCount = attemptRepository.countByEmailAndSentAtAfter(
                email,
                LocalDateTime.now().minusHours(24)
        );

        if (dailyCount >= 10) {
            throw new BadRequestException("Daily verification email limit exceeded.");
        }
    }
}
