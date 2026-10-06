package com.travelplanner.service;

import com.travelplanner.model.AuthRequest;
import com.travelplanner.model.LoginRequest;
import com.travelplanner.model.UserProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public UserService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public synchronized UserProfile register(AuthRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        String email = normalize(request.email());
        String password = request.password() == null ? "" : request.password();
        if (name.isBlank() || email.isBlank()) throw new IllegalArgumentException("Name and email are required.");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (password.length() < 6) throw new IllegalArgumentException("Password must contain at least 6 characters.");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE email = ?", Integer.class, email);
        if (count != null && count > 0) throw new IllegalArgumentException("An account with this email already exists.");
        jdbc.update("INSERT INTO app_users(email, full_name, password_hash) VALUES (?, ?, ?)",
                email, name, passwordEncoder.encode(password));
        return new UserProfile(name, email, 0);
    }

    public String login(LoginRequest request) {
        String email = normalize(request.email());
        List<String[]> rows = jdbc.query("SELECT full_name, password_hash FROM app_users WHERE email = ?",
                (rs, n) -> new String[]{rs.getString("full_name"), rs.getString("password_hash")}, email);
        if (rows.isEmpty() || !passwordEncoder.matches(request.password(), rows.get(0)[1])) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        String token = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO auth_sessions(token, user_email) VALUES (?, ?)", token, email);
        return token;
    }

    public Optional<UserProfile> profile(String token) {
        String email = emailFromToken(token);
        if (email == null) return Optional.empty();
        List<UserProfile> profiles = jdbc.query(
                "SELECT u.full_name, u.email, (SELECT COUNT(*) FROM trip_history t WHERE t.user_email=u.email) AS trip_count FROM app_users u WHERE u.email = ?",
                (rs, n) -> new UserProfile(rs.getString("full_name"), rs.getString("email"), rs.getInt("trip_count")), email);
        return profiles.stream().findFirst();
    }

    public String emailFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        String cleanToken = token.replace("Bearer ", "").trim();
        List<String> emails = jdbc.query(
                "SELECT user_email FROM auth_sessions WHERE token = ?",
                (rs, n) -> rs.getString("user_email"), cleanToken);
        return emails.stream().findFirst().orElse(null);
    }

    public void addTrip(String token) { /* Count is derived from persisted trip history. */ }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jdbc.update("DELETE FROM auth_sessions WHERE token = ?", token.replace("Bearer ", "").trim());
        }
    }

    private String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
