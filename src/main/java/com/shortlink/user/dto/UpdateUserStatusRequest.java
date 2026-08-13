package com.shortlink.user.dto;

import jakarta.validation.constraints.NotNull;

// Request DTO for updating a user's enabled status.
public record UpdateUserStatusRequest(
    @NotNull(message = "Enabled status must be specified")
    Boolean enabled
) {}
