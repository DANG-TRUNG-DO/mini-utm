package com.portfolio.mini_utm.alert.application.rule;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		name = "mini-utm.alert.telemetry-loss.enabled",
		havingValue = "true",
		matchIfMissing = true)
public class TelemetryLossScheduler {

	private final TelemetryLossMonitor monitor;

	public TelemetryLossScheduler(TelemetryLossMonitor monitor) {
		this.monitor = monitor;
	}

	@Scheduled(
			fixedDelayString = "${mini-utm.alert.telemetry-loss.scan-interval:30s}",
			initialDelayString = "${mini-utm.alert.telemetry-loss.scan-interval:30s}")
	public void detectMissingTelemetry() {
		monitor.scan(Instant.now());
	}
}
