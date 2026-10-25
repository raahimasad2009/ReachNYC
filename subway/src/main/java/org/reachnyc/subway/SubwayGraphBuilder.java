package org.reachnyc.subway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// builds the graph using weekday trains from 2pm to 7pm, since that's when kids go to programs
public class SubwayGraphBuilder {

    public static class Options {
        public String serviceId = "Weekday";
        public int windowStartSeconds = 14 * 3600;
        public int windowEndSeconds = 19 * 3600;
        public int minWaitSeconds = 60;
        public int maxWaitSeconds = 15 * 60;
        public int minRideSeconds = 30;
        public int defaultTransferSeconds = 180;

        public int windowSeconds() {
            return windowEndSeconds - windowStartSeconds;
        }
    }

    public static SubwayGraph build(SubwaySchedule feed) {
        return build(feed, new Options());
    }

    public static SubwayGraph build(SubwaySchedule feed, Options o) {
        SubwayGraph.Builder g = new SubwayGraph.Builder();

        List<String> stationIds = new ArrayList<>();
        for (SubwaySchedule.Stop s : feed.stops().values()) {
            if (s.isStation()) stationIds.add(s.id);
        }
        Collections.sort(stationIds);
        for (String id : stationIds) {
            g.station(id, feed.stops().get(id).name);
        }

        Map<String, List<SubwaySchedule.StopTime>> byTrip = readStopTimes(feed, o);

        // key looks like "A41N>A42N|A"
        Map<String, List<Integer>> rideTimes = new TreeMap<>();
        // key looks like "A41N|A"
        Map<String, Integer> departures = new TreeMap<>();

        for (String tripId : byTrip.keySet()) {
            String route = feed.trips().get(tripId).routeId;
            List<SubwaySchedule.StopTime> times = byTrip.get(tripId);
            times.sort((a, b) -> a.sequence - b.sequence);

            for (int i = 0; i < times.size() - 1; i++) {
                SubwaySchedule.StopTime a = times.get(i);
                SubwaySchedule.StopTime b = times.get(i + 1);
                if (a.departureSeconds < o.windowStartSeconds || a.departureSeconds >= o.windowEndSeconds) continue;

                String segment = a.stopId + ">" + b.stopId + "|" + route;
                if (!rideTimes.containsKey(segment)) rideTimes.put(segment, new ArrayList<>());
                int seconds = Math.max(o.minRideSeconds, b.arrivalSeconds - a.departureSeconds);
                rideTimes.get(segment).add(seconds);

                String platform = a.stopId + "|" + route;
                departures.put(platform, departures.getOrDefault(platform, 0) + 1);
            }
        }

        Set<String> done = new HashSet<>();
        for (String segment : rideTimes.keySet()) {
            int bar = segment.lastIndexOf('|');
            String route = segment.substring(bar + 1);
            String[] platforms = segment.substring(0, bar).split(">");

            int from = g.platformRoute(feed.stationOf(platforms[0]), platforms[0], route);
            int to = g.platformRoute(feed.stationOf(platforms[1]), platforms[1], route);
            g.edge(from, to, median(rideTimes.get(segment)), SubwayGraph.EdgeKind.RIDE, route);

            for (String platform : platforms) {
                String key = platform + "|" + route;
                if (done.contains(key)) continue;
                done.add(key);

                int station = g.station(feed.stationOf(platform), stationName(feed, platform));
                int onTrain = g.platformRoute(feed.stationOf(platform), platform, route);

                // if no train leaves from here (like the last stop) you can't get on here
                int count = departures.getOrDefault(key, 0);
                if (count > 0) {
                    int wait = clamp(o.windowSeconds() / count / 2, o.minWaitSeconds, o.maxWaitSeconds);
                    g.edge(station, onTrain, wait, SubwayGraph.EdgeKind.BOARD, route);
                }
                g.edge(onTrain, station, 0, SubwayGraph.EdgeKind.ALIGHT, "");
            }
        }

        Set<String> walked = new HashSet<>();
        for (SubwaySchedule.Transfer t : feed.transfers()) {
            String a = feed.stationOf(t.fromStopId);
            String b = feed.stationOf(t.toStopId);
            if (a.equals(b)) continue;
            int seconds = t.minTransferSeconds;
            if (seconds <= 0) seconds = o.defaultTransferSeconds;
            addTransfer(g, a, b, seconds, walked);
            addTransfer(g, b, a, seconds, walked);
        }

        return g.build();
    }

    private static Map<String, List<SubwaySchedule.StopTime>> readStopTimes(SubwaySchedule feed, Options o) {
        Map<String, List<SubwaySchedule.StopTime>> byTrip = new HashMap<>();
        try {
            CsvReader csv = feed.openStopTimes();
            CsvReader.Row row;
            while ((row = csv.next()) != null) {
                SubwaySchedule.StopTime st = SubwaySchedule.toStopTime(row);
                SubwaySchedule.Trip trip = feed.trips().get(st.tripId);
                if (trip == null || !trip.serviceId.equals(o.serviceId)) continue;
                if (!byTrip.containsKey(st.tripId)) byTrip.put(st.tripId, new ArrayList<>());
                byTrip.get(st.tripId).add(st);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return byTrip;
    }

    private static void addTransfer(SubwayGraph.Builder g, String from, String to, int seconds, Set<String> walked) {
        String key = from + ">" + to;
        if (walked.contains(key)) return;
        walked.add(key);
        int a = g.station(from, from);
        int b = g.station(to, to);
        g.edge(a, b, seconds, SubwayGraph.EdgeKind.TRANSFER, "");
    }

    private static String stationName(SubwaySchedule feed, String platform) {
        SubwaySchedule.Stop s = feed.stops().get(feed.stationOf(platform));
        if (s == null) return platform;
        return s.name;
    }

    static int median(List<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }

    static int clamp(int value, int low, int high) {
        if (value < low) return low;
        if (value > high) return high;
        return value;
    }
}
