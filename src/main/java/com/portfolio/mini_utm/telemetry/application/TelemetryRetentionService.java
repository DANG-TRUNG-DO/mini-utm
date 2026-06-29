package com.portfolio.mini_utm.telemetry.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

@Service
public class TelemetryRetentionService {

	private final TelemetryRepository telemetryRepository;
	private final TelemetryRetentionProperties properties;

	public TelemetryRetentionService(
			TelemetryRepository telemetryRepository,
			TelemetryRetentionProperties properties) {
		this.telemetryRepository = telemetryRepository;
		this.properties = properties;
	}

	public int purgeExpiredTelemetry(Instant now) {
		Instant cutoff = now.minus(properties.days(), ChronoUnit.DAYS);
		int totalDeleted = 0;
		int deleted;

		do {
			deleted = telemetryRepository.deleteBatchRecordedBefore(
					cutoff, properties.batchSize());
			totalDeleted += deleted;
		} while (deleted == properties.batchSize());

		return totalDeleted;
	}
}
