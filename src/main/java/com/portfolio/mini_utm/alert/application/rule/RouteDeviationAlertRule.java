package com.portfolio.mini_utm.alert.application.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.application.RaiseAlertCommand;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.mission.repository.MissionRepository;
import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

@Component
public class RouteDeviationAlertRule implements TelemetryAlertRule {

	private final MissionRepository missionRepository;
	private final AlertService alertService;
	private final RouteDeviationAlertProperties properties;

	public RouteDeviationAlertRule(
			MissionRepository missionRepository,
			AlertService alertService,
			RouteDeviationAlertProperties properties) {
		this.missionRepository = missionRepository;
		this.alertService = alertService;
		this.properties = properties;
	}

	@Override
	public void evaluate(TelemetryResponse telemetry) {
		List<RaiseAlertCommand> activeAlerts = missionDistance(telemetry)
				.filter(distance -> distance.compareTo(properties.corridorWidthMeters()) > 0)
				.map(distance -> List.of(command(telemetry, distance)))
				.orElseGet(List::of);

		alertService.reconcileActive(
				telemetry.droneId(),
				AlertType.ROUTE_DEVIATION,
				activeAlerts,
				telemetry.recordedAt());
	}

	private Optional<BigDecimal> missionDistance(TelemetryResponse telemetry) {
		if (telemetry.missionId() == null) {
			return Optional.empty();
		}
		return missionRepository.findHorizontalDistanceMeters(
				telemetry.missionId(),
				telemetry.droneId(),
				telemetry.longitude(),
				telemetry.latitude())
				.map(BigDecimal::valueOf);
	}

	private RaiseAlertCommand command(TelemetryResponse telemetry, BigDecimal distanceMeters) {
		UUID missionId = telemetry.missionId();
		return new RaiseAlertCommand(
				telemetry.droneId(),
				missionId,
				null,
				AlertType.ROUTE_DEVIATION,
				AlertSeverity.WARNING,
				missionId.toString(),
				"Drone deviated %s m from the planned route (allowed corridor: %s m)"
						.formatted(
								distanceMeters.setScale(1, RoundingMode.HALF_UP),
								properties.corridorWidthMeters().stripTrailingZeros().toPlainString()),
				telemetry.recordedAt());
	}
}
