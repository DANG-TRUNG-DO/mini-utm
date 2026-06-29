package com.portfolio.mini_utm;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.application.RaiseAlertCommand;
import com.portfolio.mini_utm.alert.application.RaiseAlertResult;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.repository.DroneRepository;

class AlertApiIntegrationTests extends PostgresIntegrationTest {

	private static final Instant BASE_TIME = Instant.parse("2025-07-01T03:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AlertService alertService;

	@Autowired
	private AlertRepository alertRepository;

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
	void filterAndPageAlertsNewestFirst() throws Exception {
		Drone droneA = drone("ALERT-API-UAV-001");
		Drone droneB = drone("ALERT-API-UAV-002");
		raise(droneA, AlertType.LOW_BATTERY, AlertSeverity.WARNING,
				"battery", BASE_TIME);
		RaiseAlertResult newest = raise(
				droneA,
				AlertType.GEOFENCE_VIOLATION,
				AlertSeverity.CRITICAL,
				"geofence",
				BASE_TIME.plusSeconds(20));
		raise(droneB, AlertType.LOW_BATTERY, AlertSeverity.WARNING,
				"battery", BASE_TIME.plusSeconds(10));

		mockMvc.perform(get("/api/v1/alerts")
					.param("droneId", droneA.getId().toString())
					.param("status", AlertStatus.OPEN.name())
					.param("page", "0")
					.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id")
						.value(newest.alert().getId().toString()))
				.andExpect(jsonPath("$.content[0].type")
						.value(AlertType.GEOFENCE_VIOLATION.name()))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(false));

		mockMvc.perform(get("/api/v1/alerts")
					.param("type", AlertType.LOW_BATTERY.name())
					.param("severity", AlertSeverity.WARNING.name()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void getAcknowledgeAndResolveAlert() throws Exception {
		Drone drone = drone("ALERT-API-UAV-003");
		RaiseAlertResult raised = raise(
				drone,
				AlertType.LOW_BATTERY,
				AlertSeverity.WARNING,
				"battery",
				BASE_TIME);
		UUID alertId = raised.alert().getId();

		mockMvc.perform(get("/api/v1/alerts/{id}", alertId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.droneId").value(drone.getId().toString()))
				.andExpect(jsonPath("$.status").value(AlertStatus.OPEN.name()))
				.andExpect(jsonPath("$.occurrenceCount").value(1));

		mockMvc.perform(post("/api/v1/alerts/{id}/acknowledge", alertId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(AlertStatus.ACKNOWLEDGED.name()))
				.andExpect(jsonPath("$.acknowledgedAt").isNotEmpty());

		mockMvc.perform(post("/api/v1/alerts/{id}/resolve", alertId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(AlertStatus.RESOLVED.name()))
				.andExpect(jsonPath("$.resolvedAt").isNotEmpty());

		mockMvc.perform(post("/api/v1/alerts/{id}/acknowledge", alertId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Alert conflict"));
	}

	@Test
	void returnNotFoundForUnknownAlert() throws Exception {
		mockMvc.perform(get("/api/v1/alerts/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Alert not found"));

		mockMvc.perform(post("/api/v1/alerts/{id}/resolve", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectInvalidPagination() throws Exception {
		mockMvc.perform(get("/api/v1/alerts").param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail")
						.value("page must be greater than or equal to 0"));

		mockMvc.perform(get("/api/v1/alerts").param("size", "201"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("size must be between 1 and 200"));
	}

	private RaiseAlertResult raise(
			Drone drone,
			AlertType type,
			AlertSeverity severity,
			String dedupKey,
			Instant detectedAt) {
		return alertService.raiseOrRefresh(new RaiseAlertCommand(
				drone.getId(),
				null,
				null,
				type,
				severity,
				dedupKey,
				"Alert API integration test",
				detectedAt));
	}

	private Drone drone(String serialNumber) {
		return droneRepository.saveAndFlush(
				new Drone(serialNumber, "Alert API drone", "Quad-X"));
	}

	private void cleanDatabase() {
		alertRepository.deleteAll();
		alertRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
