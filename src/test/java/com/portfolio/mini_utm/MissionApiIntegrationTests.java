package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;

class MissionApiIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private DroneRepository droneRepository;

	@Autowired
	private GeofenceRepository geofenceRepository;

	@BeforeEach
	void cleanBefore() {
		cleanDatabase();
	}

	@AfterEach
	void cleanAfter() {
		cleanDatabase();
	}

	@Test
	void createListAndGetMissionWithThreeDimensionalWaypoints() throws Exception {
		Drone drone = activeDrone("MISSION-UAV-001");

		UUID missionId = createMission(drone.getId(), "SGN-SURVEY");

		mockMvc.perform(get("/api/v1/missions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(missionId.toString()));

		mockMvc.perform(get("/api/v1/missions/{id}", missionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.droneId").value(drone.getId().toString()))
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andExpect(jsonPath("$.waypoints", hasSize(2)))
				.andExpect(jsonPath("$.waypoints[0].sequenceNumber").value(0))
				.andExpect(jsonPath("$.waypoints[0].altitudeM").value(60.0));

		Mission persisted = missionRepository.findDetailsById(missionId).orElseThrow();
		assertThat(persisted.getPlannedPath().getSRID()).isEqualTo(4326);
		assertThat(persisted.getPlannedPath().getCoordinates()[0].getZ()).isEqualTo(60.0);
	}

	@Test
	void approveStartCompleteAndCancelMissionsUsingValidLifecycle() throws Exception {
		Drone drone = activeDrone("MISSION-UAV-002");
		UUID missionId = createMission(drone.getId(), "MISSION-LIFECYCLE");

		performAction(missionId, "approve", "APPROVED", "approvedAt");
		performAction(missionId, "start", "ACTIVE", "startedAt");
		performAction(missionId, "complete", "COMPLETED", "completedAt");

		mockMvc.perform(post("/api/v1/missions/{id}/cancel", missionId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Mission conflict"));

		UUID cancelledId = createMission(drone.getId(), "MISSION-CANCEL");
		mockMvc.perform(post("/api/v1/missions/{id}/cancel", cancelledId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void rejectInactiveDroneInvalidTimeAndMissingMission() throws Exception {
		Drone inactiveDrone = droneRepository.saveAndFlush(new Drone("MISSION-UAV-003", "Inactive", "Quad-X"));

		mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validMissionRequest(inactiveDrone.getId(), "INACTIVE-MISSION")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Drone must be ACTIVE")));

		Drone activeDrone = activeDrone("MISSION-UAV-004");
		mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validMissionRequest(activeDrone.getId(), "INVALID-TIME")
							.replace("2026-07-01T02:00:00Z", "2026-07-01T00:00:00Z")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("plannedStartAt must be before plannedEndAt"));

		mockMvc.perform(get("/api/v1/missions/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectMissionWaypointInsideActiveGeofenceDuringOverlappingPeriod() throws Exception {
		Drone drone = activeDrone("MISSION-UAV-005");
		geofenceRepository.saveAndFlush(new Geofence(
				"MISSION-BLOCKED-ZONE",
				"Blocks the first waypoint",
				blockedArea(),
				new BigDecimal("50.00"),
				new BigDecimal("100.00"),
				true,
				Instant.parse("2026-07-01T00:30:00Z"),
				Instant.parse("2026-07-01T01:30:00Z")));

		mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validMissionRequest(drone.getId(), "BLOCKED-MISSION")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						org.hamcrest.Matchers.containsString("Waypoint 0 intersects active geofence(s): MISSION-BLOCKED-ZONE")));
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Mission drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private UUID createMission(UUID droneId, String name) throws Exception {
		String response = mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validMissionRequest(droneId, name)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).get("id").asText());
	}

	private void performAction(UUID missionId, String action, String expectedStatus, String timestampField)
			throws Exception {
		mockMvc.perform(post("/api/v1/missions/{id}/{action}", missionId, action))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andExpect(jsonPath("$." + timestampField).isNotEmpty());
	}

	private String validMissionRequest(UUID droneId, String name) {
		return """
				{
				  "droneId":"%s",
				  "name":"%s",
				  "plannedStartAt":"2026-07-01T01:00:00Z",
				  "plannedEndAt":"2026-07-01T02:00:00Z",
				  "waypoints":[
				    {"longitude":106.695,"latitude":10.775,"altitudeM":60.00},
				    {"longitude":106.710,"latitude":10.790,"altitudeM":80.00}
				  ]
				}
				""".formatted(droneId, name);
	}

	private Polygon blockedArea() {
		return GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(106.690, 10.770),
				new Coordinate(106.700, 10.770),
				new Coordinate(106.700, 10.780),
				new Coordinate(106.690, 10.780),
				new Coordinate(106.690, 10.770)
		});
	}

	private void cleanDatabase() {
		missionRepository.deleteAll();
		missionRepository.flush();
		geofenceRepository.deleteAll();
		geofenceRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
