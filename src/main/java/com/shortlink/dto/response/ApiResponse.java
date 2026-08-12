package com.shortlink.dto.response;

// Standard generic API response payload.
public record ApiResponse(
    boolean success,
    String message
) {
    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message);
    }
}
