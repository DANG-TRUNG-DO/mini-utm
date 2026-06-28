package com.portfolio.mini_utm.geofence.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGeofenceRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 2000) String description,
		@NotNull @Valid GeoJsonPolygon boundary,
		@Digits(integer = 8, fraction = 2) BigDecimal minAltitudeM,
		@Digits(integer = 8, fraction = 2) BigDecimal maxAltitudeM,
		@NotNull Boolean active,
		Instant validFrom,
		Instant validUntil) {
}
