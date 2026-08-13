package com.shortlink.constants;

// Constants defining security endpoint patterns and authorization rules across the application.
public final class SecurityEndpoints {

    private SecurityEndpoints() {
        // Prevent instantiation
    }

    // Publicly accessible auth and actuator endpoints
    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/auth/register",
        "/api/auth/verify-email",
        "/api/auth/resend-verification",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/actuator/health",
        "/actuator/info"
    };

    // Public URL API endpoints
    public static final String[] PUBLIC_URL_ENDPOINTS = {
        "/api/v1/urls/**",
        "/api/urls/**"
    };

    // Public short URL redirect path pattern
    public static final String REDIRECT_ENDPOINT = "/{shortCode:[a-zA-Z0-9_-]+}";

    // Endpoints requiring ADMIN role
    public static final String[] ADMIN_ENDPOINTS = {
        "/api/admin/**"
    };

    // Endpoints requiring authentication
    public static final String[] AUTHENTICATED_ENDPOINTS = {
        "/api/auth/me",
        "/api/auth/profile",
        "/api/auth/logout",
        "/api/auth/logout-all",
        "/api/auth/change-password",
        "/api/auth/request-deletion",
        "/api/auth/account",
        "/api/analytics/**"
    };
}
