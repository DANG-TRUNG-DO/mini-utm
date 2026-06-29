package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;

import jakarta.persistence.EntityManager;

class AlertRepositoryIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private DroneRepository droneRepository;

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private GeofenceRepository geofenceRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@Transactional
	void persistAlertWithAllRelationshipsAndLifecycleTimestamps() {
		Drone drone = droneRepository.saveAndFlush(
				new Drone("ALERT-REPO-UAV", "Alert repository drone", "Quad-X"));
		Mission mission = missionRepository.saveAndFlush(mission(drone));
		Geofence geofence = geofenceRepository.saveAndFlush(geofence());
		Instant detectedAt = Instant.parse("2026-07-01T03:00:00Z");
		Instant acknowledgedAt = detectedAt.plusSeconds(30);
		Instant resolvedAt = detectedAt.plusSeconds(60);
		Alert alert = new Alert(
				drone,
				mission,
				geofence,
				AlertType.GEOFENCE_VIOLATION,
				AlertSeverity.CRITICAL,
				geofence.getId().toString(),
				"Drone entered a restricted geofence",
				detectedAt);
		alert.acknowledge(acknowledgedAt);
		alert.resolve(resolvedAt);

		Alert saved = alertRepository.saveAndFlush(alert);
		entityManager.clear();

		Alert reloaded = alertRepository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getDrone().getId()).isEqualTo(drone.getId());
		assertThat(reloaded.getMission().getId()).isEqualTo(mission.getId());
		assertThat(reloaded.getGeofence().getId()).isEqualTo(geofence.getId());
		assertThat(reloaded.getType()).isEqualTo(AlertType.GEOFENCE_VIOLATION);
		assertThat(reloaded.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(reloaded.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(reloaded.getDedupKey()).isEqualTo(geofence.getId().toString());
		assertThat(reloaded.getDetectedAt()).isEqualTo(detectedAt);
		assertThat(reloaded.getLastDetectedAt()).isEqualTo(detectedAt);
		assertThat(reloaded.getOccurrenceCount()).isEqualTo(1);
		assertThat(reloaded.getAcknowledgedAt()).isEqualTo(acknowledgedAt);
		assertThat(reloaded.getResolvedAt()).isEqualTo(resolvedAt);
	}

	private Mission mission(Drone drone) {
		LineString path = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(106.695, 10.775, 60),
				new Coordinate(106.705, 10.785, 80)
		});
		return new Mission(
				drone,
				"Alert mission",
				path,
				Instant.parse("2026-07-01T02:30:00Z"),
				Instant.parse("2026-07-01T03:30:00Z"));
	}

	private Geofence geofence() {
		Polygon boundary = GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(106.690, 10.770),
				new Coordinate(106.710, 10.770),
				new Coordinate(106.710, 10.790),
				new Coordinate(106.690, 10.790),
				new Coordinate(106.690, 10.770)
		});
		return new Geofence(
				"ALERT-REPO-ZONE",
				"Restricted test zone",
				boundary,
				new BigDecimal("20.00"),
				new BigDecimal("120.00"),
				true,
				null,
				null);
	}
}
