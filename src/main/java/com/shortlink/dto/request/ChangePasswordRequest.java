package com.shortlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Request payload for an authenticated user to change their password.
public record ChangePasswordRequest(
    @NotBlank(message = "Current password cannot be blank")
    String currentPassword,

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
        message = "Password must contain uppercase, lowercase, digit and special character"
    )
    String newPassword,

    @NotBlank(message = "Confirm password cannot be blank")
    String confirmPassword
) {}
