package com.shortlink.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Request payload for admin login authentication.
public record LoginRequest(
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password cannot be blank")
    String password
) {}
