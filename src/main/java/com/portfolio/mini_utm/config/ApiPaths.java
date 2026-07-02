package com.portfolio.mini_utm.config;

public final class ApiPaths {

    public static final String API_V1 = "/api/v1";
    public static final String DRONES = API_V1 + "/drones";
    public static final String GEOFENCES = API_V1 + "/geofences";
    public static final String MISSIONS = API_V1 + "/missions";
    public static final String TELEMETRY = API_V1 + "/telemetry";
    public static final String ALERTS = API_V1 + "/alerts";
    public static final String DRONE_TELEMETRY = DRONES + "/{droneId}/telemetry";

    private ApiPaths() {
    }
}
