package org.reachnyc.subway;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// builds the parts of subway-times.js that the website uses to show stations and line colors
public class JsonOutput {

    static JsonObject assumptions(SubwayGraphBuilder.Options o) {
        JsonObject a = new JsonObject();
        a.addProperty("service_day", o.serviceId);
        a.addProperty("departure_window", time(o.windowStartSeconds) + "-" + time(o.windowEndSeconds));
        a.addProperty("wait_model", "half the time between trains, between "
                + o.minWaitSeconds / 60 + " and " + o.maxWaitSeconds / 60 + " min, every time you get on");
        a.addProperty("walk_speed_mps", Walking.WALK_SPEED_MPS);
        a.addProperty("street_detour_factor", Walking.STREET_DETOUR_FACTOR);
        a.addProperty("egress_radius_m", MakeSubwayTimes.NEARBY_METERS);
        a.addProperty("note", "Subway and walking only. Buses are not included yet.");
        return a;
    }

    static JsonObject linesJson(SubwaySchedule feed) {
        JsonObject lines = new JsonObject();
        for (SubwaySchedule.Route r : feed.routes().values()) {
            String color = r.color.isEmpty() ? "808183" : r.color;
            String text = r.textColor.isEmpty() ? "FFFFFF" : r.textColor;
            JsonObject line = new JsonObject();
            line.addProperty("name", r.shortName);
            line.addProperty("color", "#" + color);
            line.addProperty("text", "#" + text);
            lines.add(r.id, line);
        }
        return lines;
    }

    static JsonArray stationsJson(List<SubwaySchedule.Stop> stations, SubwayGraph graph) {
        List<SubwaySchedule.Stop> byName = new ArrayList<>(stations);
        byName.sort((a, b) -> {
            int c = a.name.compareTo(b.name);
            if (c != 0) return c;
            return a.id.compareTo(b.id);
        });

        JsonArray result = new JsonArray();
        for (SubwaySchedule.Stop s : byName) {
            JsonObject station = new JsonObject();
            station.addProperty("id", s.id);
            station.addProperty("name", s.name);
            station.addProperty("lat", round5(s.lat));
            station.addProperty("lon", round5(s.lon));
            JsonArray lines = new JsonArray();
            for (String line : MakeSubwayTimes.linesAt(graph, graph.stationNode(s.id))) lines.add(line);
            station.add("lines", lines);
            result.add(station);
        }
        return result;
    }

    static JsonObject sitesJson(List<MakeSubwayTimes.Program> programs, Map<String, List<List<MakeSubwayTimes.Exit>>> exits, SubwayGraph graph) {
        JsonObject result = new JsonObject();
        for (MakeSubwayTimes.Program p : programs) {
            if (p.sites.isEmpty()) continue;
            JsonArray sites = new JsonArray();
            List<List<MakeSubwayTimes.Exit>> perSite = exits.get(p.id);
            for (int i = 0; i < p.sites.size(); i++) {
                JsonObject site = new JsonObject();
                site.addProperty("name", p.sites.get(i).name);
                if (!perSite.get(i).isEmpty()) {
                    MakeSubwayTimes.Exit nearest = perSite.get(i).get(0);
                    site.addProperty("nearest_station", nearest.stationId);
                    site.addProperty("nearest_station_name", graph.node(nearest.node).label);
                    site.addProperty("walk_minutes", (nearest.walkSeconds + 59) / 60);
                }
                sites.add(site);
            }
            result.add(p.id, sites);
        }
        return result;
    }

    static String time(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }

    static double round5(double v) {
        return Math.round(v * 100000) / 100000.0;
    }
}
