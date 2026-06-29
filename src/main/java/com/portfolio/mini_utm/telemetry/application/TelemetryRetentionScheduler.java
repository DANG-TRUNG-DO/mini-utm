package com.portfolio.mini_utm.telemetry.application;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		name = "mini-utm.telemetry.retention.enabled",
		havingValue = "true",
		matchIfMissing = true)
public class TelemetryRetentionScheduler {

	private static final Logger log = LoggerFactory.getLogger(TelemetryRetentionScheduler.class);

	private final TelemetryRetentionService retentionService;

	public TelemetryRetentionScheduler(TelemetryRetentionService retentionService) {
		this.retentionService = retentionService;
	}

	@Scheduled(
			cron = "${mini-utm.telemetry.retention.cron:0 0 3 * * *}",
			zone = "UTC")
	public void purgeExpiredTelemetry() {
		int deleted = retentionService.purgeExpiredTelemetry(Instant.now());
		if (deleted > 0) {
			log.info("Deleted {} expired telemetry records", deleted);
		}
	}
}
