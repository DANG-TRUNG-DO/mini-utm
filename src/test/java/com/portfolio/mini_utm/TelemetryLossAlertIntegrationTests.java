package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.portfolio.mini_utm.alert.application.rule.TelemetryLossMonitor;
import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.telemetry.domain.Telemetry;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class TelemetryLossAlertIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private TelemetryLossMonitor monitor;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AlertRepository alertRepository;

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
	void createRefreshAndResolveTelemetryLossAlert() throws Exception {
		Instant observedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		Drone drone = saveDrone("LOSS-UAV-001", DroneStatus.ACTIVE);
		saveTelemetry(drone, observedAt.minusSeconds(61));

		monitor.scan(observedAt);
		Alert created = onlyAlert();
		assertThat(created.getType()).isEqualTo(AlertType.TELEMETRY_LOSS);
		assertThat(created.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(created.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(created.getOccurrenceCount()).isEqualTo(1);
		assertThat(created.getMessage()).contains(drone.getSerialNumber());

		monitor.scan(observedAt.plusSeconds(30));
		Alert refreshed = onlyAlert();
		assertThat(refreshed.getId()).isEqualTo(created.getId());
		assertThat(refreshed.getOccurrenceCount()).isEqualTo(2);
		assertThat(refreshed.getLastDetectedAt()).isEqualTo(observedAt.plusSeconds(30));

		ingest(drone, observedAt.plusSeconds(31));
		Alert resolved = onlyAlert();
		assertThat(resolved.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(resolved.getResolvedAt()).isNotNull();
		assertThat(alertRepository.count()).isEqualTo(1);
	}

	@Test
	void ignoreInactiveDronesAndActiveDronesWithRecentTelemetry() {
		Instant observedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		Drone inactive = saveDrone("LOSS-UAV-002", DroneStatus.INACTIVE);
		Drone healthy = saveDrone("LOSS-UAV-003", DroneStatus.ACTIVE);
		saveTelemetry(inactive, observedAt.minusSeconds(120));
		saveTelemetry(healthy, observedAt.minusSeconds(30));

		monitor.scan(observedAt);

		assertThat(alertRepository.count()).isZero();
	}

	private void ingest(Drone drone, Instant recordedAt) throws Exception {
		String request = """
				{
				  "droneId":"%s",
				  "longitude":106.700,
				  "latitude":10.780,
				  "altitudeM":75.50,
				  "speedMps":12.345,
				  "headingDegrees":181.50,
				  "batteryPercent":80.00,
				  "recordedAt":"%s"
				}
				""".formatted(drone.getId(), recordedAt);

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated());
	}

	private Alert onlyAlert() {
		assertThat(alertRepository.count()).isEqualTo(1);
		return alertRepository.findAll().get(0);
	}

	private Drone saveDrone(String serialNumber, DroneStatus status) {
		Drone drone = new Drone(serialNumber, "Telemetry loss drone", "Quad-X");
		if (status == DroneStatus.ACTIVE) {
			drone.transitionTo(DroneStatus.ACTIVE);
		}
		return droneRepository.saveAndFlush(drone);
	}

	private void saveTelemetry(Drone drone, Instant recordedAt) {
		var point = GEOMETRY_FACTORY.createPoint(new Coordinate(106.700, 10.780, 75.5));
		telemetryRepository.saveAndFlush(new Telemetry(
				drone,
				null,
				recordedAt,
				point,
				new BigDecimal("12.345"),
				new BigDecimal("181.50"),
				new BigDecimal("80.00")));
	}

	private void cleanDatabase() {
		alertRepository.deleteAll();
		alertRepository.flush();
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
