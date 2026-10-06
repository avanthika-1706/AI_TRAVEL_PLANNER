package com.travelplanner.service;

import com.travelplanner.model.Place;
import com.travelplanner.model.RouteEdge;
import com.travelplanner.model.TripRequest;
import com.travelplanner.model.TripResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TripPlannerService {

    private final CsvDataService dataService;
    private final AStarService aStarService;

    public TripPlannerService(CsvDataService dataService, AStarService aStarService) {
        this.dataService = dataService;
        this.aStarService = aStarService;
    }

    public TripResponse plan(TripRequest request) {
        String preference = request.preference() == null || request.preference().isBlank()
                ? "distance"
                : request.preference();

        AStarService.SearchResult result =
                aStarService.search(request.start(), request.destination(), preference);

        List<String> path = result.path();

        double distance = 0;
        double time = 0;
        double cost = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            RouteEdge edge = findEdge(path.get(i), path.get(i + 1));
            distance += edge.distanceKm();
            time += edge.travelTimeHours();
            cost += edge.travelCost();
        }

        boolean withinBudget = request.budget() <= 0 || cost <= request.budget();

        List<TripResponse.DayPlan> itinerary =
                createItinerary(path, request.days());

        return new TripResponse(
                request.start(),
                request.destination(),
                normalizePreference(preference),
                path,
                round(distance),
                round(time),
                round(cost),
                withinBudget,
                itinerary
        );
    }

    public Collection<Place> getPlaces() {
        return dataService.getPlaces().values();
    }

    private RouteEdge findEdge(String from, String to) {
        for (RouteEdge edge : dataService.getRoutes()) {
            if ((edge.fromPlace().equals(from) && edge.toPlace().equals(to))
                    || (edge.fromPlace().equals(to) && edge.toPlace().equals(from))) {
                return edge;
            }
        }
        throw new IllegalStateException("Route data missing for " + from + " → " + to);
    }

    private List<TripResponse.DayPlan> createItinerary(List<String> path, int days) {
        int usableDays = Math.max(1, days);
        List<TripResponse.DayPlan> result = new ArrayList<>();

        // Start and destination are included. Intermediate stops are distributed
        // across the requested days.
        List<String> stops = new ArrayList<>(path);

        for (int day = 1; day <= usableDays; day++) {
            List<String> dayPlaces = new ArrayList<>();

            int startIndex = (int) Math.floor((double) (day - 1) * stops.size() / usableDays);
            int endIndex = (int) Math.floor((double) day * stops.size() / usableDays);

            for (int i = startIndex; i < endIndex; i++) {
                dayPlaces.add(stops.get(i));
            }

            if (dayPlaces.isEmpty()) {
                dayPlaces.add(stops.get(Math.min(day - 1, stops.size() - 1)));
            }

            result.add(new TripResponse.DayPlan(day, dayPlaces));
        }

        return result;
    }

    private String normalizePreference(String preference) {
        String p = preference.toLowerCase(Locale.ROOT);
        if (p.contains("time") || p.contains("fast")) return "time";
        if (p.contains("cost") || p.contains("cheap")) return "cost";
        return "distance";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
