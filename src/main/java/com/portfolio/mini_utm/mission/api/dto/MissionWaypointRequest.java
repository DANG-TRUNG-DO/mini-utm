package com.portfolio.mini_utm.mission.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record MissionWaypointRequest(
		@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
		@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
		@NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal altitudeM) {
}
