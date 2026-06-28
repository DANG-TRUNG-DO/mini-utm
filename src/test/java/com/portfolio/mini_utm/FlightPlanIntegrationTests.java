package com.portfolio.mini_utm;

import static org.hamcrest.Matchers.containsString;
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

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.mission.repository.MissionRepository;

class FlightPlanIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private MockMvc mockMvc;

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
	void rejectFlightSegmentCrossingGeofenceWhenBothWaypointsAreOutside() throws Exception {
		Drone drone = activeDrone("FLIGHT-UAV-001");
		createBlockingGeofence();

		mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content(flightPlan(drone.getId(), "CROSSING-FLIGHT", 60, false)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(containsString(
						"Flight segment 0-1 intersects active geofence(s): SEGMENT-BLOCKED-ZONE")));
	}

	@Test
	void allowFlightSegmentAboveGeofenceAltitude() throws Exception {
		Drone drone = activeDrone("FLIGHT-UAV-002");
		createBlockingGeofence();

		mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content(flightPlan(drone.getId(), "HIGH-FLIGHT", 150, false)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.waypoints[0].altitudeM").value(150.0));
	}

	@Test
	void rejectIdenticalConsecutiveWaypoints() throws Exception {
		Drone drone = activeDrone("FLIGHT-UAV-003");

		mockMvc.perform(post("/api/v1/missions")
					.contentType(MediaType.APPLICATION_JSON)
					.content(flightPlan(drone.getId(), "ZERO-LENGTH-FLIGHT", 60, true)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Consecutive waypoints must not be identical"));
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Flight plan drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private void createBlockingGeofence() {
		Polygon boundary = GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(106.690, 10.770),
				new Coordinate(106.700, 10.770),
				new Coordinate(106.700, 10.780),
				new Coordinate(106.690, 10.780),
				new Coordinate(106.690, 10.770)
		});
		geofenceRepository.saveAndFlush(new Geofence(
				"SEGMENT-BLOCKED-ZONE",
				"Both endpoints are outside, but the segment crosses this polygon",
				boundary,
				new BigDecimal("50.00"),
				new BigDecimal("100.00"),
				true,
				Instant.parse("2026-07-01T00:30:00Z"),
				Instant.parse("2026-07-01T01:30:00Z")));
	}

	private String flightPlan(UUID droneId, String name, double altitude, boolean duplicateWaypoints) {
		double endLongitude = duplicateWaypoints ? 106.685 : 106.705;
		return """
				{
				  "droneId":"%s",
				  "name":"%s",
				  "plannedStartAt":"2026-07-01T01:00:00Z",
				  "plannedEndAt":"2026-07-01T02:00:00Z",
				  "waypoints":[
				    {"longitude":106.685,"latitude":10.775,"altitudeM":%s},
				    {"longitude":%s,"latitude":10.775,"altitudeM":%s}
				  ]
				}
				""".formatted(droneId, name, altitude, endLongitude, altitude);
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
