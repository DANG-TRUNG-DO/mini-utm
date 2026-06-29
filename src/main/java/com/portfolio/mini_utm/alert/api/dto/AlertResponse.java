package com.portfolio.mini_utm.alert.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;

public record AlertResponse(
		UUID id,
		UUID droneId,
		UUID missionId,
		UUID geofenceId,
		AlertType type,
		AlertSeverity severity,
		AlertStatus status,
		String message,
		String dedupKey,
		Instant detectedAt,
		Instant lastDetectedAt,
		int occurrenceCount,
		Instant acknowledgedAt,
		Instant resolvedAt) {

	public static AlertResponse from(Alert alert) {
		return new AlertResponse(
				alert.getId(),
				alert.getDrone().getId(),
				alert.getMission() == null ? null : alert.getMission().getId(),
				alert.getGeofence() == null ? null : alert.getGeofence().getId(),
				alert.getType(),
				alert.getSeverity(),
				alert.getStatus(),
				alert.getMessage(),
				alert.getDedupKey(),
				alert.getDetectedAt(),
				alert.getLastDetectedAt(),
				alert.getOccurrenceCount(),
				alert.getAcknowledgedAt(),
				alert.getResolvedAt());
	}
}
