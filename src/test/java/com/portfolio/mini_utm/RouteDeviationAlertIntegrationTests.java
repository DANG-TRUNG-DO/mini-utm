package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class RouteDeviationAlertIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private TelemetryRepository telemetryRepository;

	@Autowired
	private MissionRepository missionRepository;

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
	void createRefreshAndResolveRouteDeviationAlert() throws Exception {
		Instant baseTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
		Drone drone = activeDrone("ROUTE-UAV-001");
		Mission mission = mission(drone, "ACTIVE-ROUTE", baseTime, true);

		ingest(drone, mission, 106.700, 10.782, baseTime);
		Alert created = onlyAlert();
		assertThat(created.getType()).isEqualTo(AlertType.ROUTE_DEVIATION);
		assertThat(created.getSeverity()).isEqualTo(AlertSeverity.WARNING);
		assertThat(created.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(created.getMission().getId()).isEqualTo(mission.getId());
		assertThat(created.getDedupKey()).isEqualTo(mission.getId().toString());
		assertThat(created.getMessage()).contains("planned route", "100 m");

		ingest(drone, mission, 106.700, 10.783, baseTime.plusSeconds(10));
		Alert refreshed = onlyAlert();
		assertThat(refreshed.getId()).isEqualTo(created.getId());
		assertThat(refreshed.getOccurrenceCount()).isEqualTo(2);
		assertThat(refreshed.getLastDetectedAt()).isEqualTo(baseTime.plusSeconds(10));

		ingest(drone, mission, 106.700, 10.7805, baseTime.plusSeconds(20));
		Alert resolved = onlyAlert();
		assertThat(resolved.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(resolved.getResolvedAt()).isEqualTo(baseTime.plusSeconds(20));
		assertThat(alertRepository.count()).isEqualTo(1);
	}

	@Test
	void ignoreTelemetryWithoutAnActiveMission() throws Exception {
		Instant baseTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
		Drone drone = activeDrone("ROUTE-UAV-002");
		Mission plannedMission = mission(drone, "PLANNED-ROUTE", baseTime, false);

		ingest(drone, plannedMission, 106.700, 10.783, baseTime);
		ingest(drone, null, 106.700, 10.784, baseTime.plusSeconds(10));

		assertThat(alertRepository.count()).isZero();
	}

	private void ingest(
			Drone drone,
			Mission mission,
			double longitude,
			double latitude,
			Instant recordedAt) throws Exception {
		String missionProperty = mission == null
				? ""
				: "\"missionId\":\"%s\",".formatted(mission.getId());
		String request = """
				{
				  "droneId":"%s",
				  %s
				  "longitude":%s,
				  "latitude":%s,
				  "altitudeM":75.00,
				  "speedMps":12.345,
				  "headingDegrees":181.50,
				  "batteryPercent":80.00,
				  "recordedAt":"%s"
				}
				""".formatted(
					drone.getId(), missionProperty, longitude, latitude, recordedAt);

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
		Drone drone = new Drone(serialNumber, "Route alert drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private Mission mission(Drone drone, String name, Instant baseTime, boolean active) {
		var path = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(106.690, 10.780, 70.0),
			new Coordinate(106.710, 10.780, 80.0)
		});
		Mission mission = new Mission(
				drone,
				name,
				path,
				baseTime.minusSeconds(60),
				baseTime.plusSeconds(600));
		if (active) {
			mission.approve(baseTime.minusSeconds(30));
			mission.start(baseTime.minusSeconds(10));
		}
		return missionRepository.saveAndFlush(mission);
	}

	private void cleanDatabase() {
		alertRepository.deleteAll();
		alertRepository.flush();
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		missionRepository.deleteAll();
		missionRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
