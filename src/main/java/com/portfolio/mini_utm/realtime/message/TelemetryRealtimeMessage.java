package com.portfolio.mini_utm.realtime.message;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

public record TelemetryRealtimeMessage(
		Long telemetryId,
		UUID droneId,
		UUID missionId,
		double longitude,
		double latitude,
		BigDecimal altitudeM,
		BigDecimal speedMps,
		BigDecimal headingDegrees,
		BigDecimal batteryPercent,
		Instant recordedAt) {

	public static TelemetryRealtimeMessage from(TelemetryResponse telemetry) {
		return new TelemetryRealtimeMessage(
				telemetry.id(),
				telemetry.droneId(),
				telemetry.missionId(),
				telemetry.longitude(),
				telemetry.latitude(),
				telemetry.altitudeM(),
				telemetry.speedMps(),
				telemetry.headingDegrees(),
				telemetry.batteryPercent(),
				telemetry.recordedAt());
	}
}
