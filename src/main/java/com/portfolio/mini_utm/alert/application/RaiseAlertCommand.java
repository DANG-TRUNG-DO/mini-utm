package com.portfolio.mini_utm.alert.application;

import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertType;

public record RaiseAlertCommand(
		UUID droneId,
		UUID missionId,
		UUID geofenceId,
		AlertType type,
		AlertSeverity severity,
		String dedupKey,
		String message,
		Instant detectedAt) {
}
