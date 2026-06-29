package com.portfolio.mini_utm.realtime.message;

import java.time.Instant;
import java.util.UUID;

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
		Instant acknowledgedAt,
		Instant resolvedAt) {
}
