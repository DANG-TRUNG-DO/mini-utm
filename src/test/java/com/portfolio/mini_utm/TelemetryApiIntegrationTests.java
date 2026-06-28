package com.portfolio.mini_utm;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class TelemetryApiIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private MockMvc mockMvc;

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
	void ingestTelemetryWithDroneAndMission() throws Exception {
		Drone drone = activeDrone("INGEST-UAV-001");
		Mission mission = mission(drone, "INGEST-MISSION");

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest(drone.getId(), mission.getId(), "2026-07-01T01:15:00Z")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/telemetry/\\d+")))
				.andExpect(jsonPath("$.droneId").value(drone.getId().toString()))
				.andExpect(jsonPath("$.missionId").value(mission.getId().toString()))
				.andExpect(jsonPath("$.longitude").value(106.7))
				.andExpect(jsonPath("$.latitude").value(10.78))
				.andExpect(jsonPath("$.altitudeM").value(75.5))
				.andExpect(jsonPath("$.speedMps").value(12.345))
				.andExpect(jsonPath("$.headingDegrees").value(181.5))
				.andExpect(jsonPath("$.batteryPercent").value(82.25));
	}

	@Test
	void ingestTelemetryWithoutMissionAndRejectDuplicateTimestamp() throws Exception {
		Drone drone = activeDrone("INGEST-UAV-002");
		String request = validRequest(drone.getId(), null, "2026-07-01T01:20:00Z");

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.missionId").doesNotExist());

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Telemetry conflict"));
	}

	@Test
	void rejectInvalidMeasurementsAndUnknownDrone() throws Exception {
		Drone drone = activeDrone("INGEST-UAV-003");
		String invalid = validRequest(drone.getId(), null, "2026-07-01T01:25:00Z")
				.replace("\"speedMps\":12.345", "\"speedMps\":-1")
				.replace("\"headingDegrees\":181.50", "\"headingDegrees\":360")
				.replace("\"batteryPercent\":82.25", "\"batteryPercent\":101");

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalid))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest(UUID.randomUUID(), null, "2026-07-01T01:25:00Z")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Drone not found"));
	}

	@Test
	void rejectMissionBelongingToAnotherDrone() throws Exception {
		Drone telemetryDrone = activeDrone("INGEST-UAV-004");
		Drone missionDrone = activeDrone("INGEST-UAV-005");
		Mission mission = mission(missionDrone, "OTHER-DRONE-MISSION");

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest(telemetryDrone.getId(), mission.getId(), "2026-07-01T01:30:00Z")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(containsString("Mission does not belong")));
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Telemetry ingest drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private Mission mission(Drone drone, String name) {
		LineString path = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(106.695, 10.775, 60),
				new Coordinate(106.705, 10.785, 80)
		});
		return missionRepository.saveAndFlush(new Mission(
				drone,
				name,
				path,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T02:00:00Z")));
	}

	private String validRequest(UUID droneId, UUID missionId, String recordedAt) {
		String missionProperty = missionId == null ? "" : "\"missionId\":\"%s\",".formatted(missionId);
		return """
				{
				  "droneId":"%s",
				  %s
				  "longitude":106.700,
				  "latitude":10.780,
				  "altitudeM":75.50,
				  "speedMps":12.345,
				  "headingDegrees":181.50,
				  "batteryPercent":82.25,
				  "recordedAt":"%s"
				}
				""".formatted(droneId, missionProperty, recordedAt);
	}

	private void cleanDatabase() {
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		missionRepository.deleteAll();
		missionRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
