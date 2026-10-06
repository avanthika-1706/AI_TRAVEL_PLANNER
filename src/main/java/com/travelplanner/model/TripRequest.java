package com.travelplanner.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TripRequest(
        @NotBlank String start,
        @NotBlank String destination,
        @Min(1) int days,
        @Min(0) double budget,
        String preference
) {}
