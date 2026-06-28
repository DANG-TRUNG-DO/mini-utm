package com.portfolio.mini_utm.geofence.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CheckGeofenceRequest(
		@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
		@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
		@NotNull @Digits(integer = 8, fraction = 2) BigDecimal altitudeM,
		@NotNull Instant checkedAt) {
}
