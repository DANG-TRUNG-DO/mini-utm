package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

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

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class GeofenceViolationAlertIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);
	private static final Instant BASE_TIME = Instant.parse("2026-07-01T03:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private TelemetryRepository telemetryRepository;

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
	void createRefreshAndResolveAlertAsDroneMovesThroughGeofence() throws Exception {
		Drone drone = activeDrone("GEOFENCE-ALERT-UAV-001");
		Geofence geofence = saveGeofence(
				"ACTIVE-RESTRICTED-ZONE",
				true,
				new BigDecimal("20.00"),
				new BigDecimal("120.00"),
				BASE_TIME.minusSeconds(60),
				BASE_TIME.plusSeconds(600));

		ingest(drone, 106.700, 10.780, "75.00", BASE_TIME);
		Alert created = onlyAlert();
		assertThat(created.getType()).isEqualTo(AlertType.GEOFENCE_VIOLATION);
		assertThat(created.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(created.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(created.getGeofence().getId()).isEqualTo(geofence.getId());
		assertThat(created.getDedupKey()).isEqualTo(geofence.getId().toString());
		assertThat(created.getOccurrenceCount()).isEqualTo(1);

		ingest(drone, 106.701, 10.781, "80.00", BASE_TIME.plusSeconds(10));
		Alert refreshed = onlyAlert();
		assertThat(refreshed.getId()).isEqualTo(created.getId());
		assertThat(refreshed.getOccurrenceCount()).isEqualTo(2);
		assertThat(refreshed.getLastDetectedAt()).isEqualTo(BASE_TIME.plusSeconds(10));

		ingest(drone, 106.750, 10.850, "80.00", BASE_TIME.plusSeconds(20));
		Alert resolved = onlyAlert();
		assertThat(resolved.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(resolved.getResolvedAt()).isEqualTo(BASE_TIME.plusSeconds(20));
	}

	@Test
	void ignoreInactiveExpiredAndAltitudeMismatchedGeofences() throws Exception {
		Drone drone = activeDrone("GEOFENCE-ALERT-UAV-002");
		saveGeofence("INACTIVE-ZONE", false, null, null, null, null);
		saveGeofence(
				"EXPIRED-ZONE",
				true,
				null,
				null,
				BASE_TIME.minusSeconds(120),
				BASE_TIME.minusSeconds(60));
		saveGeofence(
				"LOW-ALTITUDE-ZONE",
				true,
				new BigDecimal("20.00"),
				new BigDecimal("120.00"),
				null,
				null);

		ingest(drone, 106.700, 10.780, "150.00", BASE_TIME);

		assertThat(alertRepository.count()).isZero();
	}

	private void ingest(
			Drone drone,
			double longitude,
			double latitude,
			String altitudeM,
			Instant recordedAt) throws Exception {
		String request = """
				{
				  "droneId":"%s",
				  "longitude":%s,
				  "latitude":%s,
				  "altitudeM":%s,
				  "speedMps":12.345,
				  "headingDegrees":181.50,
				  "batteryPercent":80.00,
				  "recordedAt":"%s"
				}
				""".formatted(drone.getId(), longitude, latitude, altitudeM, recordedAt);

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated());
	}

	private Alert onlyAlert() {
		assertThat(alertRepository.count()).isEqualTo(1);
		Alert alert = alertRepository.findAll().get(0);
		return alertRepository.findDetailsById(alert.getId()).orElseThrow();
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Geofence alert drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private Geofence saveGeofence(
			String name,
			boolean active,
			BigDecimal minAltitudeM,
			BigDecimal maxAltitudeM,
			Instant validFrom,
			Instant validUntil) {
		Polygon boundary = GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(106.690, 10.770),
				new Coordinate(106.710, 10.770),
				new Coordinate(106.710, 10.790),
				new Coordinate(106.690, 10.790),
				new Coordinate(106.690, 10.770)
		});
		return geofenceRepository.saveAndFlush(new Geofence(
				name,
				"Geofence alert integration test",
				boundary,
				minAltitudeM,
				maxAltitudeM,
				active,
				validFrom,
				validUntil));
	}

	private void cleanDatabase() {
		alertRepository.deleteAll();
		alertRepository.flush();
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		geofenceRepository.deleteAll();
		geofenceRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
