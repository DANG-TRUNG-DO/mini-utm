package com.portfolio.mini_utm.geofence.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateGeofenceRequest(
		@Pattern(regexp = ".*\\S.*", message = "must contain a non-whitespace character")
		@Size(max = 150) String name,
		@Size(max = 2000) String description,
		@Valid GeoJsonPolygon boundary,
		@Digits(integer = 8, fraction = 2) BigDecimal minAltitudeM,
		@Digits(integer = 8, fraction = 2) BigDecimal maxAltitudeM,
		Boolean active,
		Instant validFrom,
		Instant validUntil) {
}
