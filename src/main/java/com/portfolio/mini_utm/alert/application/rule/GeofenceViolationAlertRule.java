package com.portfolio.mini_utm.alert.application.rule;

import java.util.List;

import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.application.RaiseAlertCommand;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

@Component
public class GeofenceViolationAlertRule implements TelemetryAlertRule {

	private final GeofenceRepository geofenceRepository;
	private final AlertService alertService;

	public GeofenceViolationAlertRule(
			GeofenceRepository geofenceRepository,
			AlertService alertService) {
		this.geofenceRepository = geofenceRepository;
		this.alertService = alertService;
	}

	@Override
	public void evaluate(TelemetryResponse telemetry) {
		List<RaiseAlertCommand> violations = geofenceRepository.findRestrictionsCovering(
				telemetry.longitude(),
				telemetry.latitude(),
				telemetry.altitudeM(),
				telemetry.recordedAt())
				.stream()
				.map(geofence -> command(telemetry, geofence))
				.toList();

		alertService.reconcileActive(
				telemetry.droneId(),
				AlertType.GEOFENCE_VIOLATION,
				violations,
				telemetry.recordedAt());
	}

	private RaiseAlertCommand command(TelemetryResponse telemetry, Geofence geofence) {
		return new RaiseAlertCommand(
				telemetry.droneId(),
				telemetry.missionId(),
				geofence.getId(),
				AlertType.GEOFENCE_VIOLATION,
				AlertSeverity.CRITICAL,
				geofence.getId().toString(),
				"Drone entered restricted geofence: " + geofence.getName(),
				telemetry.recordedAt());
	}
}
