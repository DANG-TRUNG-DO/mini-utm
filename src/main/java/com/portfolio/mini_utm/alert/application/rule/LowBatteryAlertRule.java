package com.portfolio.mini_utm.alert.application.rule;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.application.RaiseAlertCommand;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

@Component
public class LowBatteryAlertRule implements TelemetryAlertRule {

	public static final String DEDUP_KEY = "battery-threshold";

	private final AlertService alertService;
	private final LowBatteryAlertProperties properties;

	public LowBatteryAlertRule(
			AlertService alertService,
			LowBatteryAlertProperties properties) {
		this.alertService = alertService;
		this.properties = properties;
	}

	@Override
	public void evaluate(TelemetryResponse telemetry) {
		BigDecimal batteryPercent = telemetry.batteryPercent();
		if (batteryPercent == null) {
			return;
		}

		if (batteryPercent.compareTo(properties.thresholdPercent()) <= 0) {
			alertService.raiseOrRefresh(new RaiseAlertCommand(
					telemetry.droneId(),
					telemetry.missionId(),
					null,
					AlertType.LOW_BATTERY,
					AlertSeverity.WARNING,
					DEDUP_KEY,
					"Drone battery is low: %s%%".formatted(format(batteryPercent)),
					telemetry.recordedAt()));
			return;
		}

		if (batteryPercent.compareTo(properties.recoveryThresholdPercent()) >= 0) {
			alertService.resolveActive(
					telemetry.droneId(),
					AlertType.LOW_BATTERY,
					DEDUP_KEY,
					telemetry.recordedAt());
		}
	}

	private String format(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}
}
