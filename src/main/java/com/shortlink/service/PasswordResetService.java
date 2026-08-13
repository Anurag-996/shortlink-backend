package com.shortlink.service;

import com.shortlink.dto.request.ChangePasswordRequest;
import com.shortlink.dto.request.ResetPasswordRequest;
import com.shortlink.dto.response.ApiResponse;
import com.shortlink.entity.PasswordResetToken;
import com.shortlink.exception.BadRequestException;
import com.shortlink.repository.PasswordResetTokenRepository;
import com.shortlink.repository.RefreshTokenRepository;
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
import java.util.UUID;

// Service managing forgotten password resets and credential updates with secure token hashing.
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.auth.reset-token-expiration-hours:1}")
    private int resetTokenExpirationHours;

    @Transactional
    public ApiResponse sendResetPasswordEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email cannot be blank");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user != null && user.isEnabled() && !user.isDeletionPending()) {
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = TokenHashUtil.hashToken(rawToken);

            PasswordResetToken resetToken = passwordResetTokenRepository.findByUser(user)
                    .orElseGet(() -> PasswordResetToken.builder()
                            .user(user)
                            .build());

            resetToken.setTokenHash(tokenHash);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(resetTokenExpirationHours));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            String displayName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "User";

            emailService.sendPasswordResetEmail(user.getEmail(), displayName, resetLink);
            log.info("Password reset token generated and email dispatched for user: {}", user.getEmail());
        }

        // Return non-enumerating generic response
        return ApiResponse.ok("If the account exists, a password reset link has been sent to your email.");
    }

    @Transactional
    public ApiResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (request.token() == null || request.token().isBlank()) {
            throw new BadRequestException("Invalid reset token");
        }

        String tokenHash = TokenHashUtil.hashToken(request.token().trim());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();

        // Enforce that new password must not match current password
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("Your new password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Invalidate all active sessions across devices
        refreshTokenRepository.deleteByUser(user);

        // Invalidate the reset token
        passwordResetTokenRepository.delete(resetToken);

        log.info("Password successfully reset for user [{}]", user.getEmail());
        return ApiResponse.ok("Password reset successfully. You can now log in with your new password.");
    }

    @Transactional
    public ApiResponse changePassword(User user, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("Your new password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully by user [{}]", user.getEmail());
        return ApiResponse.ok("Password changed successfully.");
    }
}
