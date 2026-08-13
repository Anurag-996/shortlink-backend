package com.shortlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Request payload for resetting a password using a verification token.
public record ResetPasswordRequest(
    @NotBlank(message = "Reset token cannot be blank")
    String token,

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
