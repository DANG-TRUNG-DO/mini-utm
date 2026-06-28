package com.portfolio.mini_utm.telemetry.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.telemetry.domain.Telemetry;

public record TelemetryResponse(
		Long id,
		UUID droneId,
		UUID missionId,
		double longitude,
		double latitude,
		BigDecimal altitudeM,
		BigDecimal speedMps,
		BigDecimal headingDegrees,
		BigDecimal batteryPercent,
		Instant recordedAt,
		Instant createdAt) {

	public static TelemetryResponse from(Telemetry telemetry) {
		return new TelemetryResponse(
				telemetry.getId(),
				telemetry.getDrone().getId(),
				telemetry.getMission() == null ? null : telemetry.getMission().getId(),
				telemetry.getPosition().getX(),
				telemetry.getPosition().getY(),
				BigDecimal.valueOf(telemetry.getPosition().getCoordinate().getZ()),
				telemetry.getSpeedMps(),
				telemetry.getHeadingDegrees(),
				telemetry.getBatteryPercent(),
				telemetry.getRecordedAt(),
				telemetry.getCreatedAt());
	}
}
