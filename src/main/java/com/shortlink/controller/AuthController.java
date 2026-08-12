package com.shortlink.controller;

import com.shortlink.dto.request.LoginRequest;
import com.shortlink.dto.response.ApiResponse;
import com.shortlink.dto.response.AuthResponse;
import com.shortlink.exception.InvalidTokenException;
import com.shortlink.security.CookieService;
import com.shortlink.service.AuthService;
import com.shortlink.service.AuthService.AuthSessionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// REST Controller exposing authentication endpoints for login, session refresh, and logout operations.
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

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
    public ResponseEntity<ApiResponse> logoutAll(
            Authentication authentication,
            HttpServletResponse response) {

        if (authentication != null && authentication.getName() != null) {
            authService.logoutAll(authentication.getName());
        }

        cookieService.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.ok("Successfully logged out from all devices"));
    }
}
