package org.reachnyc.subway;

import java.util.ArrayList;
import java.util.List;

public class TripSummary {
    public int totalSeconds;
    public int transfers;
    public List<String> lines;
    public int waitSeconds;
    public int rideSeconds;
    public int walkSeconds;

    public TripSummary(int totalSeconds, int transfers, List<String> lines, int waitSeconds, int rideSeconds, int walkSeconds) {
        this.totalSeconds = totalSeconds;
        this.transfers = transfers;
        this.lines = lines;
        this.waitSeconds = waitSeconds;
        this.rideSeconds = rideSeconds;
        this.walkSeconds = walkSeconds;
    }

    public static TripSummary of(List<SubwayGraph.Edge> path, int walkAtEndSeconds) {
        int wait = 0;
        int ride = 0;
        int walk = walkAtEndSeconds;
        int boards = 0;
        List<String> lines = new ArrayList<>();

        for (SubwayGraph.Edge e : path) {
            if (e.kind == SubwayGraph.EdgeKind.BOARD) {
                wait += e.seconds;
                boards++;
                if (lines.isEmpty() || !lines.get(lines.size() - 1).equals(e.routeId)) {
                    lines.add(e.routeId);
                }
            } else if (e.kind == SubwayGraph.EdgeKind.RIDE) {
                ride += e.seconds;
            } else if (e.kind == SubwayGraph.EdgeKind.TRANSFER) {
                walk += e.seconds;
            }
        }

        int transfers = boards - 1;
        if (transfers < 0) transfers = 0;
        return new TripSummary(wait + ride + walk, transfers, lines, wait, ride, walk);
    }

    // round up so we don't tell someone they'll make it when they won't
    public int minutes() {
        return (totalSeconds + 59) / 60;
    }
}
