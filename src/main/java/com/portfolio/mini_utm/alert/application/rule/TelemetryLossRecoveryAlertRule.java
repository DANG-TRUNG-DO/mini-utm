package com.portfolio.mini_utm.alert.application.rule;

import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TelemetryLossRecoveryAlertRule implements TelemetryAlertRule {

	private final TelemetryLossMonitor monitor;

	public TelemetryLossRecoveryAlertRule(TelemetryLossMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public void evaluate(TelemetryResponse telemetry) {
		Instant now = Instant.now();
		Instant receivedAt = telemetry.recordedAt().isAfter(now)
				? telemetry.recordedAt()
				: now;
		monitor.markTelemetryReceived(telemetry.droneId(), receivedAt);
	}
}
