package com.travelplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.model.TripResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TripHistoryService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public TripHistoryService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void save(String email, TripResponse response) {
        try {
            jdbc.update("INSERT INTO trip_history(user_email, trip_json) VALUES (?, ?)",
                    email, objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not save trip history.", e);
        }
    }

    public List<TripResponse> get(String email) {
        return jdbc.query("SELECT trip_json FROM trip_history WHERE user_email = ? ORDER BY id DESC",
                (rs, rowNum) -> {
                    try {
                        return objectMapper.readValue(rs.getString("trip_json"), TripResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Could not read saved trip history.", e);
                    }
                }, email);
    }
}
