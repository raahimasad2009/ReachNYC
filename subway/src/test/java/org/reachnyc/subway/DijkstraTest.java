package org.reachnyc.subway;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DijkstraTest {

    @Test
    void addsUpTheTimes() {
        SubwayGraph.Builder b = new SubwayGraph.Builder();
        int a = b.node("A");
        int x = b.node("B");
        int c = b.node("C");
        b.edge(a, x, 5);
        b.edge(x, c, 7);

        DijkstraResult result = Dijkstra.shortestPaths(b.build(), a);
        assertEquals(0, result.distanceTo(a));
        assertEquals(5, result.distanceTo(x));
        assertEquals(12, result.distanceTo(c));
        assertEquals(2, result.pathTo(c).size());
    }

    @Test
    void takesTheFasterWayEvenIfItsLonger() {
        SubwayGraph.Builder b = new SubwayGraph.Builder();
        int a = b.node("A");
        int x = b.node("B");
        int y = b.node("C");
        int d = b.node("D");
        b.edge(a, d, 100);
        b.edge(a, x, 1);
        b.edge(x, y, 1);
        b.edge(y, d, 1);

        DijkstraResult result = Dijkstra.shortestPaths(b.build(), a);
        assertEquals(3, result.distanceTo(d));
        assertEquals(3, result.pathTo(d).size());
    }

    @Test
    void cantReachEverything() {
        SubwayGraph.Builder b = new SubwayGraph.Builder();
        int a = b.node("A");
        int x = b.node("B");
        int alone = b.node("C");
        b.edge(x, a, 3);

        DijkstraResult result = Dijkstra.shortestPaths(b.build(), a);
        assertFalse(result.reaches(x));
        assertFalse(result.reaches(alone));
        assertTrue(result.pathTo(alone).isEmpty());
    }

    @Test
    void oneTrainBeatsALongTransfer() {
        // line 1: wait 120 + ride 1500 = 1620
        // line 2 then line 3: wait 60 + ride 600 + wait 900 + ride 300 = 1860
        SubwayGraph.Builder b = new SubwayGraph.Builder();
        int home = b.station("H", "Home");
        int middle = b.station("T", "Transfer");
        int program = b.station("P", "Program");
        int h1 = b.platformRoute("H", "H1", "1");
        int p1 = b.platformRoute("P", "P1", "1");
        int h2 = b.platformRoute("H", "H2", "2");
        int t2 = b.platformRoute("T", "T2", "2");
        int t3 = b.platformRoute("T", "T3", "3");
        int p3 = b.platformRoute("P", "P3", "3");

        b.edge(home, h1, 120, SubwayGraph.EdgeKind.BOARD, "1");
        b.edge(h1, p1, 1500, SubwayGraph.EdgeKind.RIDE, "1");
        b.edge(p1, program, 0, SubwayGraph.EdgeKind.ALIGHT, "");

        b.edge(home, h2, 60, SubwayGraph.EdgeKind.BOARD, "2");
        b.edge(h2, t2, 600, SubwayGraph.EdgeKind.RIDE, "2");
        b.edge(t2, middle, 0, SubwayGraph.EdgeKind.ALIGHT, "");
        b.edge(middle, t3, 900, SubwayGraph.EdgeKind.BOARD, "3");
        b.edge(t3, p3, 300, SubwayGraph.EdgeKind.RIDE, "3");
        b.edge(p3, program, 0, SubwayGraph.EdgeKind.ALIGHT, "");

        DijkstraResult result = Dijkstra.shortestPaths(b.build(), home);
        assertEquals(1620, result.distanceTo(program));

        TripSummary trip = TripSummary.of(result.pathTo(program), 0);
        assertEquals(List.of("1"), trip.lines);
        assertEquals(0, trip.transfers);
    }

    @Test
    void bigGraphIsFast() {
        int n = 50000;
        SubwayGraph.Builder b = new SubwayGraph.Builder();
        for (int i = 0; i < n; i++) {
            b.node("n" + i);
        }
        for (int i = 0; i < n; i++) {
            b.edge(i, (i + 1) % n, 1 + (i * 7) % 100);
            b.edge(i, (i * 31 + 17) % n, 1 + (i * 13) % 1000);
        }
        SubwayGraph graph = b.build();

        long start = System.currentTimeMillis();
        DijkstraResult result = Dijkstra.shortestPaths(graph, 0);
        long time = System.currentTimeMillis() - start;

        assertTrue(result.reaches(n - 1));
        assertTrue(time < 2000);
    }
}
