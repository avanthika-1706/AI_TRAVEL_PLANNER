package com.travelplanner.model;

public record Place(
        String placeId,
        String placeName,
        String city,
        String category,
        double latitude,
        double longitude,
        double averageVisitHours,
        double entryFee
) {}
