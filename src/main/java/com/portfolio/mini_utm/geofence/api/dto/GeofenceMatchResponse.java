package com.portfolio.mini_utm.geofence.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.geofence.domain.Geofence;

public record GeofenceMatchResponse(
		UUID id,
		String name,
		BigDecimal minAltitudeM,
		BigDecimal maxAltitudeM,
		Instant validFrom,
		Instant validUntil) {

	public static GeofenceMatchResponse from(Geofence geofence) {
		return new GeofenceMatchResponse(
				geofence.getId(),
				geofence.getName(),
				geofence.getMinAltitudeM(),
				geofence.getMaxAltitudeM(),
				geofence.getValidFrom(),
				geofence.getValidUntil());
	}
}
