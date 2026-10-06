package com.travelplanner.model;

import java.util.List;

public record TripResponse(
        String start,
        String destination,
        String preference,
        List<String> route,
        double totalDistanceKm,
        double totalTravelTimeHours,
        double totalTravelCost,
        boolean withinBudget,
        List<DayPlan> itinerary
) {
    public record DayPlan(
            int day,
            List<String> places
    ) {}
}
