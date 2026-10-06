package com.travelplanner.model;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String password
) {}
