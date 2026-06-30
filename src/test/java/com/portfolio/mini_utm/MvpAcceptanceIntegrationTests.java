package com.portfolio.mini_utm;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.mission.repository.MissionRepository;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class MvpAcceptanceIntegrationTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private TelemetryRepository telemetryRepository;

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private GeofenceRepository geofenceRepository;

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
	void operateMissionAndRecoverSafetyAlertsAcrossTheMvp() throws Exception {
		UUID droneId = registerAndActivateDrone();
		UUID missionId = createAndStartMission(droneId);
		createGeofence();

		mockMvc.perform(post("/api/v1/geofences/check")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "longitude":106.7005,
							  "latitude":10.7805,
							  "altitudeM":75.00,
							  "checkedAt":"2026-07-01T01:10:00Z"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.restricted").value(true))
				.andExpect(jsonPath("$.matches[0].name").value("MVP-RESTRICTED-ZONE"));

		ingestTelemetry(droneId, missionId, 106.7005, 10.7805, "15.00", "2026-07-01T01:10:00Z");

		mockMvc.perform(get("/api/v1/alerts")
					.param("droneId", droneId.toString())
					.param("status", "OPEN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[*].type", containsInAnyOrder(
						"LOW_BATTERY", "GEOFENCE_VIOLATION")));

		ingestTelemetry(droneId, missionId, 106.705, 10.785, "30.00", "2026-07-01T01:10:10Z");

		mockMvc.perform(get("/api/v1/drones/{droneId}/telemetry", droneId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].recordedAt").value("2026-07-01T01:10:10Z"));

		mockMvc.perform(get("/api/v1/alerts")
					.param("droneId", droneId.toString())
					.param("status", "RESOLVED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[*].type", containsInAnyOrder(
						"LOW_BATTERY", "GEOFENCE_VIOLATION")));

		mockMvc.perform(post("/api/v1/missions/{id}/complete", missionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	private UUID registerAndActivateDrone() throws Exception {
		String response = mockMvc.perform(post("/api/v1/drones")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "serialNumber":"MVP-UAV-001",
							  "name":"MVP acceptance drone",
							  "model":"Quad-X"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("INACTIVE"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		UUID droneId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

		mockMvc.perform(patch("/api/v1/drones/{id}/status", droneId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"ACTIVE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
		return droneId;
	}

	private UUID createAndStartMission(UUID droneId) throws Exception {
		String response = mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "droneId":"%s",
							  "name":"MVP-FLIGHT",
							  "plannedStartAt":"2026-07-01T01:00:00Z",
							  "plannedEndAt":"2026-07-01T02:00:00Z",
							  "waypoints":[
							    {"longitude":106.700,"latitude":10.780,"altitudeM":70.00},
							    {"longitude":106.710,"latitude":10.790,"altitudeM":80.00}
							  ]
							}
							""".formatted(droneId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		UUID missionId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

		mockMvc.perform(post("/api/v1/missions/{id}/approve", missionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
		mockMvc.perform(post("/api/v1/missions/{id}/start", missionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
		return missionId;
	}

	private void createGeofence() throws Exception {
		mockMvc.perform(post("/api/v1/geofences")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name":"MVP-RESTRICTED-ZONE",
							  "description":"Acceptance test restricted area",
							  "boundary":{"type":"Polygon","coordinates":[[
							    [106.700,10.780],
							    [106.702,10.780],
							    [106.702,10.782],
							    [106.700,10.782],
							    [106.700,10.780]
							  ]]},
							  "minAltitudeM":50.00,
							  "maxAltitudeM":100.00,
							  "active":true,
							  "validFrom":"2026-07-01T00:00:00Z",
							  "validUntil":"2026-07-01T03:00:00Z"
							}
							"""))
				.andExpect(status().isCreated());
	}

	private void ingestTelemetry(
			UUID droneId,
			UUID missionId,
			double longitude,
			double latitude,
			String batteryPercent,
			String recordedAt) throws Exception {
		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "droneId":"%s",
							  "missionId":"%s",
							  "longitude":%s,
							  "latitude":%s,
							  "altitudeM":75.00,
							  "speedMps":12.00,
							  "headingDegrees":45.00,
							  "batteryPercent":%s,
							  "recordedAt":"%s"
							}
							""".formatted(
							droneId, missionId, longitude, latitude, batteryPercent, recordedAt)))
				.andExpect(status().isCreated());
	}

	private void cleanDatabase() {
		alertRepository.deleteAll();
		alertRepository.flush();
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		missionRepository.deleteAll();
		missionRepository.flush();
		geofenceRepository.deleteAll();
		geofenceRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
