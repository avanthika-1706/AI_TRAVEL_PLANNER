# Voyage Travel Planner

A full-stack travel planning web application built with Java 17, Spring Boot, HTML/CSS/JavaScript and a CSV route dataset. It uses A* Search to plan journeys based on shortest distance, fastest estimated travel time, or cheapest estimated cost.

## Features
- Account registration and login persisted in a local file-backed H2 database.
- Passwords stored as BCrypt hashes.
- Login session tokens persisted in the database, so refreshing the browser or restarting Spring Boot does not automatically sign the user out. The Logout button ends the stored session.
- Navigation: Home, Plan Trip, My Trips, Destinations, How It Works and Profile.
- Route optimization by shortest distance, fastest travel time or cheapest estimated cost.
- Budget status, day-wise itinerary and trip history saved to the database.
- 50 destinations across India and 86 sample route connections.

## Run
Use Java 17 and Maven. Open a terminal in the folder containing `pom.xml`, then run:

```bash
mvn spring-boot:run
```

Open http://localhost:8080 in your browser.

## Demonstration: Madurai to Tirupati
The sample route data includes trade-offs so the three preferences return different paths for this journey:
- **Shortest distance:** Madurai → Pondicherry → Chennai → Tirupati — 615 km.
- **Fastest route:** Madurai → Chennai → Tirupati — about 9 hours.
- **Cheapest route:** Madurai → Coimbatore → Mysuru → Bengaluru → Tirupati — about ₹1,100.

These route values are academic/demo estimates, not live road directions, traffic conditions or current fares. Some other origin/destination pairs can legitimately share an optimal route.

## Data files
- `src/main/resources/dataset/places.csv`: 50 destinations with names, cities, categories, coordinates, visit durations and entry fees.
- `src/main/resources/dataset/routes.csv`: 86 selected route connections with distance in kilometres, estimated travel time in hours and estimated travel cost in rupees.

The network includes selected connections rather than every road in India.

## Permanent local storage
Accounts, saved trips and login sessions are stored in `voyage-data.mv.db` in the project root. Keep this file to retain data after stopping/restarting Spring Boot. The login token is also saved in this browser's local storage; clearing browser site data removes the saved browser token, but does not delete the account or trip history. Use Logout to end the stored session.

This is persistent storage on this computer, not cloud backup or automatic synchronization across computers. Do not delete `voyage-data.mv.db` if you want to retain accounts and trips.
