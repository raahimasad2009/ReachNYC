package org.reachnyc.subway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// dist[v] = fastest time to v in seconds, prevEdge[v] = the edge we took to get to v
public class DijkstraResult {

    public static final int UNREACHABLE = Integer.MAX_VALUE;

    private int source;
    private int[] dist;
    private SubwayGraph.Edge[] prevEdge;

    public DijkstraResult(int source, int[] dist, SubwayGraph.Edge[] prevEdge) {
        if (dist.length != prevEdge.length) {
            throw new IllegalArgumentException("dist and prevEdge must have one entry per node");
        }
        if (source < 0 || source >= dist.length) {
            throw new IllegalArgumentException("source " + source + " is not a node");
        }
        this.source = source;
        this.dist = dist;
        this.prevEdge = prevEdge;
    }

    public int source() {
        return source;
    }

    public int distanceTo(int node) {
        return dist[node];
    }

    public boolean reaches(int node) {
        return dist[node] != UNREACHABLE;
    }

    public SubwayGraph.Edge previousEdge(int node) {
        return prevEdge[node];
    }

    // go backwards from the target using prevEdge, then flip it
    public List<SubwayGraph.Edge> pathTo(int target) {
        List<SubwayGraph.Edge> path = new ArrayList<>();
        if (!reaches(target)) return path;
        int v = target;
        while (v != source) {
            SubwayGraph.Edge e = prevEdge[v];
            if (e == null) {
                throw new IllegalStateException("Node " + v + " is reachable but has no previous edge");
            }
            if (e.to != v) {
                throw new IllegalStateException("prevEdge[" + v + "] points to " + e.to + ", not " + v);
            }
            path.add(e);
            v = e.from;
            if (path.size() > dist.length) {
                throw new IllegalStateException("prevEdge contains a cycle");
            }
        }
        Collections.reverse(path);
        return path;
    }
}
