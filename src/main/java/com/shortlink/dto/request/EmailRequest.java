package com.shortlink.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Request payload containing an email address for verification resend and forgot password triggers.
public record EmailRequest(
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email
) {}
