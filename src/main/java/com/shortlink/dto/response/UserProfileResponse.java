package com.shortlink.dto.response;

// Response DTO containing authenticated user profile details.
public record UserProfileResponse(
    Long id,
    String name,
    String email,
    String role,
    boolean enabled
) {}
