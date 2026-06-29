package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.telemetry.application.TelemetryRetentionService;
import com.portfolio.mini_utm.telemetry.domain.Telemetry;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class TelemetryRetentionIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);
	private static final Instant NOW = Instant.parse("2026-07-31T03:00:00Z");

	@Autowired
	private TelemetryRetentionService retentionService;

	@Autowired
	private TelemetryRepository telemetryRepository;

	@Autowired
	private DroneRepository droneRepository;

	@BeforeEach
	void cleanBefore() {
		cleanDatabase();
	}

	@AfterEach
	void cleanAfter() {
		cleanDatabase();
	}

	@Test
	void deleteOnlyExpiredTelemetryInConfiguredBatches() {
		Drone drone = droneRepository.saveAndFlush(
				new Drone("RETENTION-UAV-001", "Retention drone", "Quad-X"));
		Instant cutoff = NOW.minusSeconds(30L * 24 * 60 * 60);

		saveTelemetry(drone, cutoff.minusSeconds(2));
		saveTelemetry(drone, cutoff.minusSeconds(1));
		saveTelemetry(drone, cutoff);
		saveTelemetry(drone, cutoff.plusSeconds(1));

		int deleted = retentionService.purgeExpiredTelemetry(NOW);

		assertThat(deleted).isEqualTo(2);
		assertThat(telemetryRepository.findAll())
				.extracting(Telemetry::getRecordedAt)
				.containsExactlyInAnyOrder(cutoff, cutoff.plusSeconds(1));
	}

	@Test
	void finishWhenNoTelemetryHasExpired() {
		Drone drone = droneRepository.saveAndFlush(
				new Drone("RETENTION-UAV-002", "Current telemetry drone", "Quad-X"));
		saveTelemetry(drone, NOW.minusSeconds(60));

		assertThat(retentionService.purgeExpiredTelemetry(NOW)).isZero();
		assertThat(telemetryRepository.count()).isEqualTo(1);
	}

	private void saveTelemetry(Drone drone, Instant recordedAt) {
		telemetryRepository.saveAndFlush(new Telemetry(
				drone,
				null,
				recordedAt,
				GEOMETRY_FACTORY.createPoint(new Coordinate(106.700, 10.780, 75)),
				new BigDecimal("12.345"),
				new BigDecimal("181.50"),
				new BigDecimal("82.25")));
	}

	private void cleanDatabase() {
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
