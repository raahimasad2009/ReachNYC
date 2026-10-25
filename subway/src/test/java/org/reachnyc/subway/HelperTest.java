package org.reachnyc.subway;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelperTest {

    @Test
    void csvWithCommasInQuotes() {
        List<String> fields = CsvReader.parseLine("A,\"8 Avenue, Express\",\"say \"\"hi\"\"\",");
        assertEquals("A", fields.get(0));
        assertEquals("8 Avenue, Express", fields.get(1));
        assertEquals("say \"hi\"", fields.get(2));
        assertEquals("", fields.get(3));
    }

    @Test
    void readsTimes() {
        assertEquals(15 * 3600 + 5 * 60 + 30, SubwaySchedule.parseTime("15:05:30"));
        assertEquals(25 * 3600 + 10 * 60, SubwaySchedule.parseTime("25:10:00"));
    }

    @Test
    void walkingDistance() {
        double meters = Walking.meters(40.70, -73.99, 40.71, -73.99);
        assertTrue(meters > 1100 && meters < 1120);
        assertEquals(0, Walking.walkSeconds(40.7, -73.9, 40.7, -73.9));
    }

    @Test
    void tripSummary() {
        List<SubwayGraph.Edge> path = new ArrayList<>();
        path.add(new SubwayGraph.Edge(0, 1, 120, SubwayGraph.EdgeKind.BOARD, "A"));
        path.add(new SubwayGraph.Edge(1, 2, 600, SubwayGraph.EdgeKind.RIDE, "A"));
        path.add(new SubwayGraph.Edge(2, 3, 0, SubwayGraph.EdgeKind.ALIGHT, ""));
        path.add(new SubwayGraph.Edge(3, 4, 180, SubwayGraph.EdgeKind.TRANSFER, ""));
        path.add(new SubwayGraph.Edge(4, 5, 300, SubwayGraph.EdgeKind.BOARD, "G"));
        path.add(new SubwayGraph.Edge(5, 6, 240, SubwayGraph.EdgeKind.RIDE, "G"));

        TripSummary trip = TripSummary.of(path, 300);
        assertEquals(List.of("A", "G"), trip.lines);
        assertEquals(1, trip.transfers);
        assertEquals(1740, trip.totalSeconds);
        assertEquals(29, trip.minutes());
    }

    @Test
    void rebuildsThePath() {
        SubwayGraph.Edge ab = new SubwayGraph.Edge(0, 1, 5, SubwayGraph.EdgeKind.RIDE, "A");
        SubwayGraph.Edge bc = new SubwayGraph.Edge(1, 2, 7, SubwayGraph.EdgeKind.RIDE, "A");
        int[] dist = {0, 5, 12};
        SubwayGraph.Edge[] prev = {null, ab, bc};
        DijkstraResult result = new DijkstraResult(0, dist, prev);
        assertEquals(List.of(ab, bc), result.pathTo(2));
    }

    @Test
    void noNegativeTimes() {
        assertThrows(IllegalArgumentException.class, () -> new SubwayGraph.Edge(0, 1, -1, SubwayGraph.EdgeKind.RIDE, "A"));
    }
}
