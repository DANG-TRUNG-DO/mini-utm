package com.portfolio.mini_utm.alert.application.rule;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.application.RaiseAlertCommand;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.drone.repository.DroneTelemetryActivity;

@Service
public class TelemetryLossMonitor {

	static final String DEDUP_KEY = "telemetry-signal";

	private final DroneRepository droneRepository;
	private final AlertService alertService;
	private final TelemetryLossAlertProperties properties;

	public TelemetryLossMonitor(
			DroneRepository droneRepository,
			AlertService alertService,
			TelemetryLossAlertProperties properties) {
		this.droneRepository = droneRepository;
		this.alertService = alertService;
		this.properties = properties;
	}

	public void scan(Instant observedAt) {
		Instant cutoff = observedAt.minus(properties.timeout());
		List<DroneTelemetryActivity> activities =
				droneRepository.findTelemetryActivityByStatus(DroneStatus.ACTIVE);

		for (DroneTelemetryActivity activity : activities) {
			Instant lastSeenAt = activity.getLastTelemetryAt() == null
					? activity.getMonitoringStartedAt()
					: activity.getLastTelemetryAt();
			if (!lastSeenAt.isAfter(cutoff)) {
				alertService.raiseOrRefresh(new RaiseAlertCommand(
						activity.getDroneId(),
						null,
						null,
						AlertType.TELEMETRY_LOSS,
						AlertSeverity.CRITICAL,
						DEDUP_KEY,
						"Telemetry signal lost for drone %s; last received at %s"
								.formatted(activity.getSerialNumber(), lastSeenAt),
						observedAt));
			} else {
				alertService.resolveActive(
						activity.getDroneId(),
						AlertType.TELEMETRY_LOSS,
						DEDUP_KEY,
						observedAt);
			}
		}
	}

	public void markTelemetryReceived(java.util.UUID droneId, Instant receivedAt) {
		alertService.resolveActive(
				droneId,
				AlertType.TELEMETRY_LOSS,
				DEDUP_KEY,
				receivedAt);
	}
}
