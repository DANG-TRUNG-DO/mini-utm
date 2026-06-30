package com.portfolio.mini_utm.realtime.message;

import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.alert.api.dto.AlertResponse;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;

public record AlertRealtimeMessage(
		UUID alertId,
		UUID droneId,
		UUID missionId,
		UUID geofenceId,
		AlertType type,
		AlertSeverity severity,
		AlertStatus status,
		String message,
		Instant detectedAt,
		Instant lastDetectedAt,
		int occurrenceCount,
		Instant acknowledgedAt,
		Instant resolvedAt) {

	public static AlertRealtimeMessage from(AlertResponse alert) {
		return new AlertRealtimeMessage(
				alert.id(),
				alert.droneId(),
				alert.missionId(),
				alert.geofenceId(),
				alert.type(),
				alert.severity(),
				alert.status(),
				alert.message(),
				alert.detectedAt(),
				alert.lastDetectedAt(),
				alert.occurrenceCount(),
				alert.acknowledgedAt(),
				alert.resolvedAt());
	}
}
