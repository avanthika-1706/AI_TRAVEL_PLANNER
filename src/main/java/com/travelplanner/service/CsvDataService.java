package com.travelplanner.service;

import com.travelplanner.model.Place;
import com.travelplanner.model.RouteEdge;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CsvDataService {

    private final Map<String, Place> places = new LinkedHashMap<>();
    private final List<RouteEdge> routes = new ArrayList<>();

    public CsvDataService() {
        loadPlaces();
        loadRoutes();
    }

    public Map<String, Place> getPlaces() {
        return Collections.unmodifiableMap(places);
    }

    public List<RouteEdge> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    private void loadPlaces() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("dataset/places.csv").getInputStream(),
                StandardCharsets.UTF_8))) {

            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.isBlank()) continue;

                String[] p = line.split(",", -1);
                Place place = new Place(
                        p[0].trim(),
                        p[1].trim(),
                        p[2].trim(),
                        p[3].trim(),
                        Double.parseDouble(p[4].trim()),
                        Double.parseDouble(p[5].trim()),
                        Double.parseDouble(p[6].trim()),
                        Double.parseDouble(p[7].trim())
                );
                places.put(place.placeName(), place);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load places.csv", e);
        }
    }

    private void loadRoutes() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("dataset/routes.csv").getInputStream(),
                StandardCharsets.UTF_8))) {

            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.isBlank()) continue;

                String[] p = line.split(",", -1);
                routes.add(new RouteEdge(
                        p[1].trim(),
                        p[2].trim(),
                        Double.parseDouble(p[3].trim()),
                        Double.parseDouble(p[4].trim()),
                        Double.parseDouble(p[5].trim())
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load routes.csv", e);
        }
    }
}
