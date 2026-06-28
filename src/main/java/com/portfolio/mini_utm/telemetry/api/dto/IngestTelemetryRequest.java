package com.portfolio.mini_utm.telemetry.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record IngestTelemetryRequest(
		@NotNull UUID droneId,
		UUID missionId,
		@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
		@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
		@NotNull @Digits(integer = 8, fraction = 2) BigDecimal altitudeM,
		@DecimalMin("0.0") @Digits(integer = 7, fraction = 3) BigDecimal speedMps,
		@DecimalMin("0.0") @DecimalMax(value = "360.0", inclusive = false)
		@Digits(integer = 3, fraction = 2) BigDecimal headingDegrees,
		@DecimalMin("0.0") @DecimalMax("100.0")
		@Digits(integer = 3, fraction = 2) BigDecimal batteryPercent,
		@NotNull Instant recordedAt) {
}
