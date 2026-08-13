package com.shortlink.user.dto;

import com.shortlink.user.Role;

import jakarta.validation.constraints.NotNull;

// Request DTO for updating a user's role.
public record UpdateUserRoleRequest(
    @NotNull(message = "Role must be specified")
    Role role
) {}
