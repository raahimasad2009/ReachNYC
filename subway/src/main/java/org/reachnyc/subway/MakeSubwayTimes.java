package org.reachnyc.subway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// figures out the trip from every station to every program and saves it to data/subway-times.js
// so the website doesn't need a server
public class MakeSubwayTimes {

    static final double NEARBY_METERS = 1600;
    static final int MAX_NEARBY_STATIONS = 5;
    static final int MIN_NEARBY_STATIONS = 2;
    static final int MAX_WALK_SECONDS = 30 * 60;

    static class Site {
        public String name;
        public double lat;
        public double lon;

        public Site(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    static class Program {
        public String id;
        public List<Site> sites;

        public Program(String id, List<Site> sites) {
            this.id = id;
            this.sites = sites;
        }
    }

    static class Exit {
        public String stationId;
        public int node;
        public int walkSeconds;

        public Exit(String stationId, int node, int walkSeconds) {
            this.stationId = stationId;
            this.node = node;
            this.walkSeconds = walkSeconds;
        }
    }

    public static void main(String[] args) throws IOException {
        Path gtfsDir = Path.of("gtfs");
        Path programsFile = Path.of("../data/programs.js");
        Path outFile = Path.of("../data/subway-times.js");
        for (int i = 0; i + 1 < args.length; i += 2) {
            if (args[i].equals("--gtfs")) gtfsDir = Path.of(args[i + 1]);
            else if (args[i].equals("--programs")) programsFile = Path.of(args[i + 1]);
            else if (args[i].equals("--out")) outFile = Path.of(args[i + 1]);
        }

        long startTime = System.currentTimeMillis();
        System.out.println("Loading GTFS from " + gtfsDir.toAbsolutePath());
        SubwaySchedule feed = SubwaySchedule.load(gtfsDir);
        SubwayGraphBuilder.Options options = new SubwayGraphBuilder.Options();
        SubwayGraph graph = SubwayGraphBuilder.build(feed, options);
        System.out.println("Graph: " + graph.nodeCount() + " nodes, " + graph.edgeCount() + " edges");

        List<Program> programs = loadPrograms(programsFile);
        int inPerson = 0;
        for (Program p : programs) {
            if (!p.sites.isEmpty()) inPerson++;
        }
        System.out.println("Programs with a location: " + inPerson + " of " + programs.size());

        List<SubwaySchedule.Stop> stations = servedStations(feed, graph);

        Map<String, List<List<Exit>>> exits = new HashMap<>();
        for (Program p : programs) {
            List<List<Exit>> perSite = new ArrayList<>();
            for (Site s : p.sites) perSite.add(nearbyStations(stations, graph, s));
            exits.put(p.id, perSite);
        }

        JsonObject times = new JsonObject();
        for (SubwaySchedule.Stop origin : stations) {
            DijkstraResult sp = Dijkstra.shortestPaths(graph, graph.stationNode(origin.id));
            JsonObject row = new JsonObject();
            for (Program p : programs) {
                JsonArray best = bestTrip(origin, sp, p, exits.get(p.id));
                if (best != null) row.add(p.id, best);
            }
            times.add(origin.id, row);
        }

        JsonObject out = new JsonObject();
        out.addProperty("format", "times[stationId][programId] = [minutes, transfers, lines, siteIndex, walkMinutesAtEnd]");
        out.addProperty("generated", LocalDate.now().toString());
        out.add("feed", new Gson().toJsonTree(feed.feedInfo()));
        out.add("assumptions", JsonOutput.assumptions(options));
        out.add("lines", JsonOutput.linesJson(feed));
        out.add("stations", JsonOutput.stationsJson(stations, graph));
        out.add("sites", JsonOutput.sitesJson(programs, exits, graph));
        out.add("times", times);

        // save it as a .js file so the website can load it with a script tag
        String json = new GsonBuilder().disableHtmlEscaping().create().toJson(out);
        Files.writeString(outFile, "const subwayTimes = " + json + ";\n", StandardCharsets.UTF_8);

        double seconds = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.println("Wrote " + outFile + " in " + seconds + " seconds");
    }

    // returns [minutes, transfers, "lines", siteIndex, walkMinutesAtEnd] or null if you can't get there
    static JsonArray bestTrip(SubwaySchedule.Stop origin, DijkstraResult sp, Program program, List<List<Exit>> sites) {
        TripSummary best = null;
        int bestSite = -1;
        int bestWalk = 0;
        for (int i = 0; i < sites.size(); i++) {
            Site site = program.sites.get(i);
            int walkThere = Walking.walkSeconds(origin.lat, origin.lon, site.lat, site.lon);
            if (walkThere <= MAX_WALK_SECONDS && (best == null || walkThere < best.totalSeconds)) {
                best = TripSummary.of(new ArrayList<>(), walkThere);
                bestSite = i;
                bestWalk = walkThere;
            }
            for (Exit e : sites.get(i)) {
                if (!sp.reaches(e.node)) continue;
                long total = (long) sp.distanceTo(e.node) + e.walkSeconds;
                if (best != null && total >= best.totalSeconds) continue;
                best = TripSummary.of(sp.pathTo(e.node), e.walkSeconds);
                bestSite = i;
                bestWalk = e.walkSeconds;
            }
        }
        if (best == null) return null;
        JsonArray trip = new JsonArray();
        trip.add(best.minutes());
        trip.add(best.transfers);
        trip.add(String.join(" ", best.lines));
        trip.add(bestSite);
        trip.add((bestWalk + 59) / 60);
        return trip;
    }

    // closest stations to a program (always at least 2, even if they're far)
    static List<Exit> nearbyStations(List<SubwaySchedule.Stop> stations, SubwayGraph graph, Site site) {
        List<SubwaySchedule.Stop> sorted = new ArrayList<>(stations);
        sorted.sort((a, b) -> Double.compare(
                Walking.meters(a.lat, a.lon, site.lat, site.lon),
                Walking.meters(b.lat, b.lon, site.lat, site.lon)));

        List<Exit> result = new ArrayList<>();
        for (SubwaySchedule.Stop s : sorted) {
            double meters = Walking.meters(s.lat, s.lon, site.lat, site.lon);
            boolean closeEnough = meters <= NEARBY_METERS && result.size() < MAX_NEARBY_STATIONS;
            boolean needMore = result.size() < MIN_NEARBY_STATIONS;
            if (!closeEnough && !needMore) break;
            int walk = Walking.walkSeconds(s.lat, s.lon, site.lat, site.lon);
            result.add(new Exit(s.id, graph.stationNode(s.id), walk));
        }
        return result;
    }

    static List<SubwaySchedule.Stop> servedStations(SubwaySchedule feed, SubwayGraph graph) {
        List<SubwaySchedule.Stop> result = new ArrayList<>();
        for (String id : graph.stationIds()) {
            SubwaySchedule.Stop s = feed.stops().get(id);
            if (s != null && !linesAt(graph, graph.stationNode(id)).isEmpty()) result.add(s);
        }
        return result;
    }

    static TreeSet<String> linesAt(SubwayGraph graph, int stationNode) {
        TreeSet<String> lines = new TreeSet<>();
        for (SubwayGraph.Edge e : graph.edgesFrom(stationNode)) {
            if (e.kind == SubwayGraph.EdgeKind.BOARD) lines.add(e.routeId);
        }
        return lines;
    }

    static List<Program> loadPrograms(Path file) throws IOException {
        // programs.js starts with "const programData = " so skip to the first {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        text = text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1);
        JsonElement root = JsonParser.parseString(text);
        JsonArray list;
        if (root.isJsonArray()) list = root.getAsJsonArray();
        else list = root.getAsJsonObject().getAsJsonArray("programs");

        List<Program> programs = new ArrayList<>();
        for (JsonElement item : list) {
            JsonObject p = item.getAsJsonObject();
            List<Site> sites = new ArrayList<>();
            JsonElement locations = p.get("locations");
            if (locations != null && locations.isJsonArray()) {
                for (JsonElement l : locations.getAsJsonArray()) {
                    JsonObject loc = l.getAsJsonObject();
                    if (!loc.has("lat") || !loc.has("lon")) continue;
                    if (loc.get("lat").isJsonNull() || loc.get("lon").isJsonNull()) continue;
                    String name = loc.has("name") ? loc.get("name").getAsString() : "";
                    sites.add(new Site(name, loc.get("lat").getAsDouble(), loc.get("lon").getAsDouble()));
                }
            }
            programs.add(new Program(p.get("id").getAsString(), sites));
        }
        return programs;
    }
}
