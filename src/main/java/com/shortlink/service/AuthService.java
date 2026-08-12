package com.shortlink.service;

import com.shortlink.dto.request.LoginRequest;

// Interface defining business logic operations for authentication and token lifecycles.
public interface AuthService {

    // Authenticates admin credentials and produces new tokens.
    AuthSessionResult login(LoginRequest request);

    // Validates the existing refresh token, rotates it, and issues a fresh access token.
    AuthSessionResult refresh(String refreshTokenValue);

    // Revokes the current device refresh token from the database.
    void logout(String refreshTokenValue);

    // Revokes all active refresh tokens for the authenticated user across all devices.
    void logoutAll(String userEmail);

    record AuthSessionResult(
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshTokenMaxAgeMillis
    ) {}
}
