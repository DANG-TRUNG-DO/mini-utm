package com.portfolio.mini_utm.geofence.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.geofence.domain.Geofence;

public record GeofenceResponse(
		UUID id,
		String name,
		String description,
		GeoJsonPolygon boundary,
		BigDecimal minAltitudeM,
		BigDecimal maxAltitudeM,
		boolean active,
		Instant validFrom,
		Instant validUntil,
		Instant createdAt,
		Instant updatedAt) {

	public static GeofenceResponse from(Geofence geofence, GeoJsonPolygon boundary) {
		return new GeofenceResponse(
				geofence.getId(),
				geofence.getName(),
				geofence.getDescription(),
				boundary,
				geofence.getMinAltitudeM(),
				geofence.getMaxAltitudeM(),
				geofence.isActive(),
				geofence.getValidFrom(),
				geofence.getValidUntil(),
				geofence.getCreatedAt(),
				geofence.getUpdatedAt());
	}
}
