package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;
import com.portfolio.mini_utm.telemetry.domain.Telemetry;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

import jakarta.persistence.EntityManager;

class TelemetryRepositoryIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private TelemetryRepository telemetryRepository;

	@Autowired
	private DroneRepository droneRepository;

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@Transactional
	void persistsAndReadsThreeDimensionalTelemetryWithDroneAndMission() {
		Drone drone = new Drone("TELEMETRY-UAV-001", "Telemetry drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		droneRepository.saveAndFlush(drone);

		LineString path = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(106.695, 10.775, 60),
				new Coordinate(106.705, 10.785, 80)
		});
		Mission mission = missionRepository.saveAndFlush(new Mission(
				drone,
				"TELEMETRY-MISSION",
				path,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T02:00:00Z")));

		Point position = GEOMETRY_FACTORY.createPoint(new Coordinate(106.700, 10.780, 75.5));
		Instant recordedAt = Instant.parse("2026-07-01T01:15:00Z");
		Telemetry saved = telemetryRepository.saveAndFlush(new Telemetry(
				drone,
				mission,
				recordedAt,
				position,
				new BigDecimal("12.345"),
				new BigDecimal("181.50"),
				new BigDecimal("82.25")));
		entityManager.clear();

		Telemetry reloaded = telemetryRepository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getDrone().getId()).isEqualTo(drone.getId());
		assertThat(reloaded.getMission().getId()).isEqualTo(mission.getId());
		assertThat(reloaded.getRecordedAt()).isEqualTo(recordedAt);
		assertThat(reloaded.getPosition().getSRID()).isEqualTo(4326);
		assertThat(reloaded.getPosition().getX()).isEqualTo(106.700);
		assertThat(reloaded.getPosition().getY()).isEqualTo(10.780);
		assertThat(reloaded.getPosition().getCoordinate().getZ()).isEqualTo(75.5);
		assertThat(reloaded.getSpeedMps()).isEqualByComparingTo("12.345");
		assertThat(reloaded.getHeadingDegrees()).isEqualByComparingTo("181.50");
		assertThat(reloaded.getBatteryPercent()).isEqualByComparingTo("82.25");
		assertThat(reloaded.getCreatedAt()).isNotNull();
	}

	@Test
	void schemaHasDroneRecordedAtAndSpatialIndexes() {
		List<String> indexDefinitions = jdbcTemplate.queryForList("""
				SELECT indexdef
				FROM pg_indexes
				WHERE schemaname = 'public' AND tablename = 'telemetry'
				""", String.class);

		assertThat(indexDefinitions)
				.anySatisfy(index -> assertThat(index).contains("(drone_id, recorded_at)"))
				.anySatisfy(index -> assertThat(index).contains("USING gist (\"position\")"));
	}
}
