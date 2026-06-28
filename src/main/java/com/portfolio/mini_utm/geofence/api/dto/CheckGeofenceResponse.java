package com.portfolio.mini_utm.geofence.api.dto;

import java.util.List;

public record CheckGeofenceResponse(
		boolean restricted,
		List<GeofenceMatchResponse> matches) {
}
