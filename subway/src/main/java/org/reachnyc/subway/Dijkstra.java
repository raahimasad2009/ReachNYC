package org.reachnyc.subway;

import java.util.Arrays;
import java.util.PriorityQueue;

public class Dijkstra {

    // finds the fastest time from one station to every node in the graph
    public static DijkstraResult shortestPaths(SubwayGraph graph, int source) {
        int n = graph.nodeCount();
        int[] dist = new int[n];
        SubwayGraph.Edge[] prevEdge = new SubwayGraph.Edge[n];
        Arrays.fill(dist, DijkstraResult.UNREACHABLE);
        dist[source] = 0;

        // each entry is {distance, node}, smallest distance comes out first
        PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        queue.add(new int[]{0, source});

        while (!queue.isEmpty()) {
            int[] top = queue.poll();
            int d = top[0];
            int node = top[1];
            if (d > dist[node]) continue;

            for (SubwayGraph.Edge e : graph.edgesFrom(node)) {
                int newDist = d + e.seconds;
                if (newDist < dist[e.to]) {
                    dist[e.to] = newDist;
                    prevEdge[e.to] = e;
                    queue.add(new int[]{newDist, e.to});
                }
            }
        }

        return new DijkstraResult(source, dist, prevEdge);
    }
}
