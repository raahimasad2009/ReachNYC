package org.reachnyc.subway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SubwaySchedule {

    public static class Stop {
        public String id;
        public String name;
        public double lat;
        public double lon;
        public int locationType;
        public String parentId;

        public Stop(String id, String name, double lat, double lon, int locationType, String parentId) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.locationType = locationType;
            this.parentId = parentId;
        }

        public boolean isStation() {
            return locationType == 1 || parentId.isEmpty();
        }
    }

    public static class Route {
        public String id;
        public String shortName;
        public String color;
        public String textColor;

        public Route(String id, String shortName, String color, String textColor) {
            this.id = id;
            this.shortName = shortName;
            this.color = color;
            this.textColor = textColor;
        }
    }

    public static class Trip {
        public String id;
        public String routeId;
        public String serviceId;
        public int directionId;

        public Trip(String id, String routeId, String serviceId, int directionId) {
            this.id = id;
            this.routeId = routeId;
            this.serviceId = serviceId;
            this.directionId = directionId;
        }
    }

    public static class Transfer {
        public String fromStopId;
        public String toStopId;
        public int minTransferSeconds;

        public Transfer(String fromStopId, String toStopId, int minTransferSeconds) {
            this.fromStopId = fromStopId;
            this.toStopId = toStopId;
            this.minTransferSeconds = minTransferSeconds;
        }
    }

    public static class StopTime {
        public String tripId;
        public String stopId;
        public int arrivalSeconds;
        public int departureSeconds;
        public int sequence;

        public StopTime(String tripId, String stopId, int arrivalSeconds, int departureSeconds, int sequence) {
            this.tripId = tripId;
            this.stopId = stopId;
            this.arrivalSeconds = arrivalSeconds;
            this.departureSeconds = departureSeconds;
            this.sequence = sequence;
        }
    }

    private Path dir;
    private Map<String, Stop> stops = new LinkedHashMap<>();
    private Map<String, Route> routes = new LinkedHashMap<>();
    private Map<String, Trip> trips = new HashMap<>();
    private List<Transfer> transfers = new ArrayList<>();
    private Map<String, String> feedInfo = new LinkedHashMap<>();

    public static SubwaySchedule load(Path dir) throws IOException {
        String[] needed = {"stops.txt", "routes.txt", "trips.txt", "stop_times.txt"};
        for (String name : needed) {
            if (!Files.exists(dir.resolve(name))) {
                throw new IOException("Missing " + name + " in " + dir.toAbsolutePath()
                        + " (unzip the MTA gtfs_subway.zip into this folder)");
            }
        }

        SubwaySchedule feed = new SubwaySchedule();
        feed.dir = dir;
        CsvReader.Row r;

        CsvReader csv = new CsvReader(dir.resolve("stops.txt"));
        while ((r = csv.next()) != null) {
            Stop stop = new Stop(r.get("stop_id"), r.get("stop_name"),
                    Double.parseDouble(r.get("stop_lat")), Double.parseDouble(r.get("stop_lon")),
                    toInt(r.get("location_type"), 0), r.get("parent_station"));
            feed.stops.put(stop.id, stop);
        }

        csv = new CsvReader(dir.resolve("routes.txt"));
        while ((r = csv.next()) != null) {
            String shortName = r.get("route_short_name");
            if (shortName.isEmpty()) shortName = r.get("route_id");
            Route route = new Route(r.get("route_id"), shortName, r.get("route_color"), r.get("route_text_color"));
            feed.routes.put(route.id, route);
        }

        csv = new CsvReader(dir.resolve("trips.txt"));
        while ((r = csv.next()) != null) {
            Trip trip = new Trip(r.get("trip_id"), r.get("route_id"), r.get("service_id"), toInt(r.get("direction_id"), 0));
            feed.trips.put(trip.id, trip);
        }

        if (Files.exists(dir.resolve("transfers.txt"))) {
            csv = new CsvReader(dir.resolve("transfers.txt"));
            while ((r = csv.next()) != null) {
                feed.transfers.add(new Transfer(r.get("from_stop_id"), r.get("to_stop_id"), toInt(r.get("min_transfer_time"), 180)));
            }
        }

        if (Files.exists(dir.resolve("feed_info.txt"))) {
            csv = new CsvReader(dir.resolve("feed_info.txt"));
            String[] keys = {"feed_publisher_name", "feed_start_date", "feed_end_date", "feed_version"};
            while ((r = csv.next()) != null) {
                for (String key : keys) {
                    if (r.has(key)) feed.feedInfo.put(key, r.get(key));
                }
            }
        }

        return feed;
    }

    // stop_times.txt is really big so we read it row by row instead of keeping it all
    public CsvReader openStopTimes() throws IOException {
        return new CsvReader(dir.resolve("stop_times.txt"));
    }

    public static StopTime toStopTime(CsvReader.Row r) {
        return new StopTime(r.get("trip_id"), r.get("stop_id"),
                parseTime(r.get("arrival_time")), parseTime(r.get("departure_time")),
                Integer.parseInt(r.get("stop_sequence")));
    }

    public String stationOf(String stopId) {
        Stop s = stops.get(stopId);
        if (s == null) return stopId;
        if (s.parentId.isEmpty()) return s.id;
        return s.parentId;
    }

    // times can go past 24:00:00 for trains that run after midnight
    public static int parseTime(String text) {
        String[] parts = text.trim().split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Bad GTFS time: '" + text + "'");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        int s = Integer.parseInt(parts[2]);
        return h * 3600 + m * 60 + s;
    }

    private static int toInt(String text, int fallback) {
        if (text == null || text.isBlank()) return fallback;
        return Integer.parseInt(text.trim());
    }

    public Map<String, Stop> stops() { return stops; }
    public Map<String, Route> routes() { return routes; }
    public Map<String, Trip> trips() { return trips; }
    public List<Transfer> transfers() { return transfers; }
    public Map<String, String> feedInfo() { return feedInfo; }
}
