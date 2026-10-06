package com.travelplanner.model;

public record AuthResponse(String token, String name, String email, String message) {}
