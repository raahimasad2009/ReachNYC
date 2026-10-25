package org.reachnyc.subway;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// test-schedule has one line A going X -> Y -> Z, and a 240 second walk between Z and W
class SubwayGraphBuilderTest {

    private SubwayGraph makeGraph() throws Exception {
        SubwaySchedule schedule = SubwaySchedule.load(Path.of("src/test/resources/test-schedule"));
        SubwayGraphBuilder.Options options = new SubwayGraphBuilder.Options();
        options.windowStartSeconds = 15 * 3600;
        options.windowEndSeconds = 16 * 3600;
        return SubwayGraphBuilder.build(schedule, options);
    }

    private SubwayGraph.Edge findEdge(SubwayGraph graph, int from, int to, SubwayGraph.EdgeKind kind) {
        for (SubwayGraph.Edge e : graph.edgesFrom(from)) {
            if (e.to == to && e.kind == kind) return e;
        }
        return null;
    }

    private int findNode(SubwayGraph graph, String label) {
        for (int i = 0; i < graph.nodeCount(); i++) {
            if (graph.node(i).label.equals(label)) return i;
        }
        return -1;
    }

    @Test
    void hasAllTheStations() throws Exception {
        SubwayGraph graph = makeGraph();
        assertTrue(graph.stationNode("X") >= 0);
        assertTrue(graph.stationNode("Y") >= 0);
        assertTrue(graph.stationNode("Z") >= 0);
        assertTrue(graph.stationNode("W") >= 0);
    }

    @Test
    void rideTimeIsTheMedian() throws Exception {
        // trips from X to Y between 3 and 4pm take 120, 150, 120, 180 seconds. the median is 135
        SubwayGraph graph = makeGraph();
        SubwayGraph.Edge ride = findEdge(graph, findNode(graph, "XN|A"), findNode(graph, "YN|A"), SubwayGraph.EdgeKind.RIDE);
        assertNotNull(ride);
        assertEquals(135, ride.seconds);
    }

    @Test
    void waitIsHalfTheTimeBetweenTrains() throws Exception {
        // 4 trains in an hour = one every 15 min, so the wait is 7.5 min
        SubwayGraph graph = makeGraph();
        SubwayGraph.Edge board = findEdge(graph, graph.stationNode("X"), findNode(graph, "XN|A"), SubwayGraph.EdgeKind.BOARD);
        assertEquals(450, board.seconds);
    }

    @Test
    void cantGetOnAtTheLastStop() throws Exception {
        SubwayGraph graph = makeGraph();
        assertNull(findEdge(graph, graph.stationNode("Z"), findNode(graph, "ZN|A"), SubwayGraph.EdgeKind.BOARD));
    }

    @Test
    void walkingTransfersGoBothWays() throws Exception {
        SubwayGraph graph = makeGraph();
        SubwayGraph.Edge zw = findEdge(graph, graph.stationNode("Z"), graph.stationNode("W"), SubwayGraph.EdgeKind.TRANSFER);
        SubwayGraph.Edge wz = findEdge(graph, graph.stationNode("W"), graph.stationNode("Z"), SubwayGraph.EdgeKind.TRANSFER);
        assertEquals(240, zw.seconds);
        assertEquals(240, wz.seconds);
    }
}
