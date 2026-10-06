package com.travelplanner.controller;

import com.travelplanner.model.*;
import com.travelplanner.service.TripHistoryService;
import com.travelplanner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final UserService userService;
    private final TripHistoryService historyService;

    public AuthController(UserService userService, TripHistoryService historyService) {
        this.userService = userService;
        this.historyService = historyService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        try {
            UserProfile profile = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = userService.login(request);
            UserProfile profile = userService.profile(token).orElseThrow();
            return ResponseEntity.ok(new AuthResponse(token, profile.name(), profile.email(), "Login successful"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return userService.profile(auth)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Please login first.")));
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestHeader(value = "Authorization", required = false) String auth) {
        String email = userService.emailFromToken(auth);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Please login first."));
        List<TripResponse> trips = historyService.get(email);
        return ResponseEntity.ok(trips);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        userService.logout(auth);
        return ResponseEntity.ok(new ErrorResponse("Logged out"));
    }

    public record ErrorResponse(String message) {}
}
