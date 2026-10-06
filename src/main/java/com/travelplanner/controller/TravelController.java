package com.travelplanner.controller;

import com.travelplanner.model.Place;
import com.travelplanner.model.TripRequest;
import com.travelplanner.model.TripResponse;
import com.travelplanner.service.TripHistoryService;
import com.travelplanner.service.TripPlannerService;
import com.travelplanner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TravelController {
    private final TripPlannerService tripPlannerService;
    private final UserService userService;
    private final TripHistoryService historyService;

    public TravelController(TripPlannerService tripPlannerService, UserService userService, TripHistoryService historyService) {
        this.tripPlannerService = tripPlannerService;
        this.userService = userService;
        this.historyService = historyService;
    }

    @GetMapping("/places")
    public Collection<Place> places() { return tripPlannerService.getPlaces(); }

    @PostMapping("/trips/plan")
    public ResponseEntity<?> plan(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody TripRequest request) {
        String email = userService.emailFromToken(auth);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Please login to generate a travel plan."));
        try {
            TripResponse response = tripPlannerService.plan(request);
            historyService.save(email, response);
            userService.addTrip(auth);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    public record ErrorResponse(String message) {}
}
