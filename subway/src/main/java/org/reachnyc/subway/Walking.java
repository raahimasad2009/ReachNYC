package org.reachnyc.subway;

public class Walking {

    public static final double EARTH_RADIUS_M = 6371000;
    public static final double WALK_SPEED_MPS = 1.33;
    // streets aren't a straight line so add 30%
    public static final double STREET_DETOUR_FACTOR = 1.3;

    // haversine formula
    public static double meters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    public static int walkSeconds(double lat1, double lon1, double lat2, double lon2) {
        return (int) Math.round(meters(lat1, lon1, lat2, lon2) * STREET_DETOUR_FACTOR / WALK_SPEED_MPS);
    }
}
