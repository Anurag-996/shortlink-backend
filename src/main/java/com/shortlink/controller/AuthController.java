package com.shortlink.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shortlink.dto.request.ChangePasswordRequest;
import com.shortlink.dto.request.EmailRequest;
import com.shortlink.dto.request.LoginRequest;
import com.shortlink.dto.request.RegisterRequest;
import com.shortlink.dto.request.ResetPasswordRequest;
import com.shortlink.dto.request.UpdateProfileRequest;
import com.shortlink.dto.response.ApiResponse;
import com.shortlink.dto.response.AuthResponse;
import com.shortlink.dto.response.UserProfileResponse;
import com.shortlink.exception.BadRequestException;
import com.shortlink.exception.InvalidTokenException;
import com.shortlink.security.CookieService;
import com.shortlink.security.util.SecurityUtils;
import com.shortlink.service.AccountDeletionService;
import com.shortlink.service.AuthService;
import com.shortlink.service.AuthService.AuthSessionResult;
import com.shortlink.service.EmailVerificationService;
import com.shortlink.service.PasswordResetService;
import com.shortlink.service.UserProfileService;
import com.shortlink.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// REST Controller exposing authentication endpoints for registration, email verification, password reset, login, and deletion.
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final AccountDeletionService accountDeletionService;
    private final UserProfileService userProfileService;
    private final CookieService cookieService;

    // GET /api/auth/me - Authenticated user profile retrieval.
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(userProfileService.getCurrentUserProfile(user.getEmail()));
    }

    // PUT /api/auth/profile - Authenticated, verified user profile name update.
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(userProfileService.updateProfileName(user.getEmail(), request));
    }

    // POST /api/auth/register - Initiates user signup, saves pending registration with hashed password & token, dispatches verification email.
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse response = emailVerificationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/auth/verify-email?token=... - Validates single-use token, creates active User with role USER, sends welcome email.
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmailGet(@RequestParam String token) {
        ApiResponse response = emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/verify-email - Alternative POST endpoint for single-page apps / JSON bodies.
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmailPost(
            @RequestParam(required = false) String token,
            @RequestBody(required = false) Map<String, String> body) {
        String tokenToVerify = token;
        if (tokenToVerify == null && body != null) {
            tokenToVerify = body.get("token");
        }
        if (tokenToVerify == null || tokenToVerify.isBlank()) {
            throw new BadRequestException("Verification token is required");
        }
        ApiResponse response = emailVerificationService.verifyEmail(tokenToVerify);
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/resend-verification - Resends verification email for pending registration with rate-limiting.
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
        ApiResponse response = emailVerificationService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/forgot-password - Dispatches password reset link with single-use hashed token.
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        ApiResponse response = passwordResetService.sendResetPasswordEmail(request.email());
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/reset-password - Resets password using verification token, ensuring new password differs from current.
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ApiResponse response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    // PUT /api/auth/change-password - Authenticated user password modification.
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        User user = SecurityUtils.getCurrentUser();
        ApiResponse response = passwordResetService.changePassword(user, request);
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/request-deletion - Authenticated USER requests account deletion (scheduled exactly 7 days later).
    @PostMapping("/request-deletion")
    public ResponseEntity<ApiResponse> requestDeletion(HttpServletResponse response) {
        User user = SecurityUtils.getCurrentUser();
        ApiResponse apiResponse = accountDeletionService.requestAccountDeletion(user);
        cookieService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(apiResponse);
    }

    // DELETE /api/auth/account - Alias for account deletion request.
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse> deleteAccount(HttpServletResponse response) {
        return requestDeletion(response);
    }

    // POST /api/auth/login - Authenticates credentials, sets HttpOnly refresh cookie, returns access JWT.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthSessionResult result = authService.login(request);
        cookieService.setRefreshTokenCookie(response, result.refreshToken(), result.refreshTokenMaxAgeMillis());

        return ResponseEntity.ok(AuthResponse.bearer(result.accessToken(), result.expiresIn()));
    }

    // POST /api/auth/refresh - Reads HttpOnly refresh cookie, rotates token, sets new cookie, returns fresh access JWT.
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = cookieService.extractRefreshToken(request)
                .orElseThrow(() -> {
                    cookieService.clearRefreshTokenCookie(response);
                    return new InvalidTokenException();
                });

        try {
            AuthSessionResult result = authService.refresh(refreshToken);
            cookieService.setRefreshTokenCookie(response, result.refreshToken(), result.refreshTokenMaxAgeMillis());
            return ResponseEntity.ok(AuthResponse.bearer(result.accessToken(), result.expiresIn()));
        } catch (Exception e) {
            cookieService.clearRefreshTokenCookie(response);
            throw e;
        }
    }

    // POST /api/auth/logout - Authenticated: Revokes current device's refresh token and clears its cookie.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        cookieService.extractRefreshToken(request).ifPresent(authService::logout);
        cookieService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.ok("Successfully logged out from current device"));
    }

    // POST /api/auth/logout-all - Authenticated: Revokes ALL active refresh tokens for the user across all devices.
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse> logoutAll(HttpServletResponse response) {
        User user = SecurityUtils.getCurrentUserOrNull();
        if (user != null) {
            authService.logoutAll(user.getEmail());
        }

        cookieService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.ok("Successfully logged out from all devices"));
    }
}