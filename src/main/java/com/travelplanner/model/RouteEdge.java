package com.travelplanner.model;

public record RouteEdge(
        String fromPlace,
        String toPlace,
        double distanceKm,
        double travelTimeHours,
        double travelCost
) {}
