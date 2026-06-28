package com.portfolio.mini_utm;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.telemetry.domain.Telemetry;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class TelemetryHistoryApiIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);
	private static final Instant BASE_TIME = Instant.parse("2026-07-01T01:00:00Z");

	@Autowired
	private MockMvc mockMvc;

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
	void returnStablePagesOrderedNewestFirstForOneDrone() throws Exception {
		Drone drone = activeDrone("HISTORY-UAV-001");
		Drone otherDrone = activeDrone("HISTORY-UAV-002");
		for (int minute = 0; minute < 5; minute++) {
			saveTelemetry(drone, minute);
		}
		saveTelemetry(otherDrone, 10);

		mockMvc.perform(get("/api/v1/drones/{droneId}/telemetry", drone.getId())
					.param("page", "0")
					.param("size", "2")
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].recordedAt").value("2026-07-01T01:04:00Z"))
				.andExpect(jsonPath("$.content[1].recordedAt").value("2026-07-01T01:03:00Z"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(5))
				.andExpect(jsonPath("$.totalPages").value(3))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(false));

		mockMvc.perform(get("/api/v1/drones/{droneId}/telemetry", drone.getId())
					.param("page", "1")
					.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].recordedAt").value("2026-07-01T01:02:00Z"))
				.andExpect(jsonPath("$.content[1].recordedAt").value("2026-07-01T01:01:00Z"))
				.andExpect(jsonPath("$.page").value(1));
	}

	@Test
	void filterHistoryUsingInclusiveFromAndExclusiveTo() throws Exception {
		Drone drone = activeDrone("HISTORY-UAV-003");
		for (int minute = 0; minute < 5; minute++) {
			saveTelemetry(drone, minute);
		}

		mockMvc.perform(get("/api/v1/drones/{droneId}/telemetry", drone.getId())
					.param("from", "2026-07-01T01:01:00Z")
					.param("to", "2026-07-01T01:04:00Z")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(3)))
				.andExpect(jsonPath("$.content[0].recordedAt").value("2026-07-01T01:03:00Z"))
				.andExpect(jsonPath("$.content[2].recordedAt").value("2026-07-01T01:01:00Z"));
	}

	@Test
	void rejectInvalidRangeAndOversizedPage() throws Exception {
		Drone drone = activeDrone("HISTORY-UAV-004");

		mockMvc.perform(get("/api/v1/drones/{droneId}/telemetry", drone.getId())
					.param("from", "2026-07-01T02:00:00Z")
					.param("to", "2026-07-01T01:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("from must be before to"));

		mockMvc.perform(get("/api/v1/drones/{droneId}/telemetry", drone.getId())
					.param("size", "201"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnNotFoundForUnknownDrone() throws Exception {
		mockMvc.perform(get("/api/v1/drones/{droneId}/telemetry", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Drone not found"));
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Telemetry history drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private void saveTelemetry(Drone drone, int minuteOffset) {
		telemetryRepository.saveAndFlush(new Telemetry(
				drone,
				null,
				BASE_TIME.plus(minuteOffset, ChronoUnit.MINUTES),
				GEOMETRY_FACTORY.createPoint(new Coordinate(106.700, 10.780, 75 + minuteOffset)),
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
