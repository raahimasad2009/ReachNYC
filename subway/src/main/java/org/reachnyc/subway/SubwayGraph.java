package org.reachnyc.subway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// nodes are stations and "on line X at platform P"
// BOARD = wait for the train, RIDE = train to next stop, ALIGHT = get off (0 sec), TRANSFER = walk between stations
public class SubwayGraph {

    public enum NodeType { STATION, PLATFORM_ROUTE }

    public enum EdgeKind { BOARD, RIDE, ALIGHT, TRANSFER }

    public static class Node {
        public int index;
        public NodeType type;
        public String stationId;
        public String routeId;
        public String label;

        public Node(int index, NodeType type, String stationId, String routeId, String label) {
            this.index = index;
            this.type = type;
            this.stationId = stationId;
            this.routeId = routeId;
            this.label = label;
        }
    }

    public static class Edge {
        public int from;
        public int to;
        public int seconds;
        public EdgeKind kind;
        public String routeId;

        public Edge(int from, int to, int seconds, EdgeKind kind, String routeId) {
            if (seconds < 0) throw new IllegalArgumentException("Edge weights must be non-negative: " + seconds);
            this.from = from;
            this.to = to;
            this.seconds = seconds;
            this.kind = kind;
            this.routeId = routeId;
        }
    }

    private List<Node> nodes;
    private List<List<Edge>> adjacency;
    private Map<String, Integer> stationIndex;

    private SubwayGraph(List<Node> nodes, List<List<Edge>> adjacency, Map<String, Integer> stationIndex) {
        this.nodes = nodes;
        this.adjacency = adjacency;
        this.stationIndex = stationIndex;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        int count = 0;
        for (List<Edge> list : adjacency) count += list.size();
        return count;
    }

    public List<Edge> edgesFrom(int node) {
        return adjacency.get(node);
    }

    public Node node(int index) {
        return nodes.get(index);
    }

    public int stationNode(String stationId) {
        Integer i = stationIndex.get(stationId);
        if (i == null) return -1;
        return i;
    }

    public List<String> stationIds() {
        List<String> ids = new ArrayList<>(stationIndex.keySet());
        Collections.sort(ids);
        return ids;
    }

    public static class Builder {
        private List<Node> nodes = new ArrayList<>();
        private List<List<Edge>> adjacency = new ArrayList<>();
        private Map<String, Integer> stationIndex = new HashMap<>();
        private Map<String, Integer> platformIndex = new HashMap<>();

        public int station(String stationId, String label) {
            if (stationIndex.containsKey(stationId)) return stationIndex.get(stationId);
            int i = add(NodeType.STATION, stationId, "", label);
            stationIndex.put(stationId, i);
            return i;
        }

        public int platformRoute(String stationId, String platformId, String routeId) {
            String key = platformId + "|" + routeId;
            if (platformIndex.containsKey(key)) return platformIndex.get(key);
            int i = add(NodeType.PLATFORM_ROUTE, stationId, routeId, key);
            platformIndex.put(key, i);
            return i;
        }

        public int node(String label) {
            return station(label, label);
        }

        public Builder edge(int from, int to, int seconds, EdgeKind kind, String routeId) {
            if (from < 0 || from >= nodes.size() || to < 0 || to >= nodes.size()) {
                throw new IndexOutOfBoundsException("No node " + from + " or " + to);
            }
            if (routeId == null) routeId = "";
            adjacency.get(from).add(new Edge(from, to, seconds, kind, routeId));
            return this;
        }

        public Builder edge(int from, int to, int seconds) {
            return edge(from, to, seconds, EdgeKind.TRANSFER, "");
        }

        public int size() {
            return nodes.size();
        }

        public SubwayGraph build() {
            return new SubwayGraph(nodes, adjacency, stationIndex);
        }

        private int add(NodeType type, String stationId, String routeId, String label) {
            int i = nodes.size();
            nodes.add(new Node(i, type, stationId, routeId, label));
            adjacency.add(new ArrayList<>());
            return i;
        }
    }
}
