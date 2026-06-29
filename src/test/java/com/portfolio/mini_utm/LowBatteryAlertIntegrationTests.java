package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class LowBatteryAlertIntegrationTests extends PostgresIntegrationTest {

	private static final Instant BASE_TIME = Instant.parse("2026-07-01T03:00:00Z");

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
	void createRefreshAndResolveLowBatteryAlertUsingHysteresis() throws Exception {
		Drone drone = activeDrone("LOW-BATTERY-UAV-001");

		ingest(drone, "20.00", BASE_TIME);
		Alert created = onlyAlert();
		assertThat(created.getType()).isEqualTo(AlertType.LOW_BATTERY);
		assertThat(created.getSeverity()).isEqualTo(AlertSeverity.WARNING);
		assertThat(created.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(created.getOccurrenceCount()).isEqualTo(1);
		assertThat(created.getMessage()).contains("20%");

		ingest(drone, "10.00", BASE_TIME.plusSeconds(10));
		Alert refreshed = onlyAlert();
		assertThat(refreshed.getId()).isEqualTo(created.getId());
		assertThat(refreshed.getOccurrenceCount()).isEqualTo(2);
		assertThat(refreshed.getLastDetectedAt()).isEqualTo(BASE_TIME.plusSeconds(10));
		assertThat(refreshed.getMessage()).contains("10%");

		ingest(drone, "22.00", BASE_TIME.plusSeconds(20));
		Alert insideHysteresisBand = onlyAlert();
		assertThat(insideHysteresisBand.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(insideHysteresisBand.getOccurrenceCount()).isEqualTo(2);

		ingest(drone, "25.00", BASE_TIME.plusSeconds(30));
		Alert resolved = onlyAlert();
		assertThat(resolved.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(resolved.getResolvedAt()).isEqualTo(BASE_TIME.plusSeconds(30));
		assertThat(alertRepository.count()).isEqualTo(1);
	}

	@Test
	void ignoreTelemetryWithoutBatteryMeasurement() throws Exception {
		Drone drone = activeDrone("LOW-BATTERY-UAV-002");

		ingest(drone, null, BASE_TIME);

		assertThat(alertRepository.count()).isZero();
	}

	private void ingest(Drone drone, String batteryPercent, Instant recordedAt) throws Exception {
		String batteryProperty = batteryPercent == null
				? ""
				: "\"batteryPercent\":%s,".formatted(batteryPercent);
		String request = """
				{
				  "droneId":"%s",
				  "longitude":106.700,
				  "latitude":10.780,
				  "altitudeM":75.50,
				  "speedMps":12.345,
				  "headingDegrees":181.50,
				  %s
				  "recordedAt":"%s"
				}
				""".formatted(drone.getId(), batteryProperty, recordedAt);

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated());
	}

	private Alert onlyAlert() {
		assertThat(alertRepository.count()).isEqualTo(1);
		return alertRepository.findAll().get(0);
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Low battery alert drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
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
