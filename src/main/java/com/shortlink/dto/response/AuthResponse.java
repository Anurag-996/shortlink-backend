package com.shortlink.dto.response;

// Response payload returned upon successful authentication containing access token details.
public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {
    public static AuthResponse bearer(String accessToken, long expiresIn) {
        return new AuthResponse(accessToken, "Bearer", expiresIn);
    }
}
