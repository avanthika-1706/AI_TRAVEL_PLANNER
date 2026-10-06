package com.travelplanner.service;

import com.travelplanner.model.Place;
import com.travelplanner.model.RouteEdge;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AStarService {

    private final CsvDataService dataService;
    private final Map<String, List<RouteEdge>> graph = new HashMap<>();
    // Lower-bound ratios keep the A* heuristic admissible even when the
    // sample road distances are shorter than the straight-line distance.
    private double minDistancePerStraightKm = Double.POSITIVE_INFINITY;
    private double minTimePerStraightKm = Double.POSITIVE_INFINITY;
    private double minCostPerStraightKm = Double.POSITIVE_INFINITY;

    public AStarService(CsvDataService dataService) {
        this.dataService = dataService;
        buildGraph();
    }

    private void buildGraph() {
        for (String place : dataService.getPlaces().keySet()) {
            graph.put(place, new ArrayList<>());
        }

        // The CSV routes are treated as bidirectional.
        for (RouteEdge edge : dataService.getRoutes()) {
            graph.get(edge.fromPlace()).add(edge);
            graph.get(edge.toPlace()).add(new RouteEdge(
                    edge.toPlace(),
                    edge.fromPlace(),
                    edge.distanceKm(),
                    edge.travelTimeHours(),
                    edge.travelCost()
            ));

            Place from = dataService.getPlaces().get(edge.fromPlace());
            Place to = dataService.getPlaces().get(edge.toPlace());
            double straightLineKm = haversineKm(
                    from.latitude(), from.longitude(), to.latitude(), to.longitude()
            );
            if (straightLineKm > 0) {
                minDistancePerStraightKm = Math.min(minDistancePerStraightKm,
                        edge.distanceKm() / straightLineKm);
                minTimePerStraightKm = Math.min(minTimePerStraightKm,
                        edge.travelTimeHours() / straightLineKm);
                minCostPerStraightKm = Math.min(minCostPerStraightKm,
                        edge.travelCost() / straightLineKm);
            }
        }
    }

    public SearchResult search(String start, String goal, String preference) {
        if (!graph.containsKey(start) || !graph.containsKey(goal)) {
            throw new IllegalArgumentException("Start or destination is not present in the dataset.");
        }

        String mode = normalizePreference(preference);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::fScore));
        Map<String, Double> gScore = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> closed = new HashSet<>();

        gScore.put(start, 0.0);
        open.add(new Node(start, 0.0, heuristic(start, goal, mode)));

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (closed.contains(current.place())) continue;
            closed.add(current.place());

            if (current.place().equals(goal)) {
                List<String> path = reconstructPath(parent, start, goal);
                return new SearchResult(path, gScore.get(goal), closed.size());
            }

            for (RouteEdge edge : graph.getOrDefault(current.place(), List.of())) {
                String next = edge.toPlace();
                if (closed.contains(next)) continue;

                double edgeCost = edgeCost(edge, mode);
                double tentativeG = gScore.get(current.place()) + edgeCost;

                if (tentativeG < gScore.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    gScore.put(next, tentativeG);
                    parent.put(next, current.place());

                    double h = heuristic(next, goal, mode);
                    open.add(new Node(next, tentativeG, tentativeG + h));
                }
            }
        }

        throw new IllegalArgumentException("No route exists between " + start + " and " + goal + ".");
    }

    private String normalizePreference(String preference) {
        if (preference == null) return "distance";

        return switch (preference.toLowerCase(Locale.ROOT)) {
            case "time", "fastest", "fastest route" -> "time";
            case "cost", "cheapest", "cheapest route" -> "cost";
            default -> "distance";
        };
    }

    private double edgeCost(RouteEdge edge, String mode) {
        return switch (mode) {
            case "time" -> edge.travelTimeHours();
            case "cost" -> edge.travelCost();
            default -> edge.distanceKm();
        };
    }

    private double heuristic(String from, String goal, String mode) {
        Place a = dataService.getPlaces().get(from);
        Place b = dataService.getPlaces().get(goal);
        double straightLineDistance = haversineKm(
                a.latitude(), a.longitude(), b.latitude(), b.longitude()
        );

        return switch (mode) {
            case "time" -> straightLineDistance * safeLowerBound(minTimePerStraightKm);
            case "cost" -> straightLineDistance * safeLowerBound(minCostPerStraightKm);
            default -> straightLineDistance * safeLowerBound(minDistancePerStraightKm);
        };
    }

    private double safeLowerBound(double ratio) {
        return Double.isFinite(ratio) && ratio > 0 ? ratio : 0.0;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private List<String> reconstructPath(
            Map<String, String> parent,
            String start,
            String goal
    ) {
        LinkedList<String> path = new LinkedList<>();
        String current = goal;

        while (current != null) {
            path.addFirst(current);
            if (current.equals(start)) break;
            current = parent.get(current);
        }

        if (!path.getFirst().equals(start)) {
            throw new IllegalStateException("Could not reconstruct A* path.");
        }

        return path;
    }

    public record SearchResult(
            List<String> path,
            double optimizedScore,
            int nodesExplored
    ) {}

    private record Node(
            String place,
            double gScore,
            double fScore
    ) {}
}
