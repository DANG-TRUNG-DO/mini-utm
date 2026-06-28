package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;

import jakarta.persistence.EntityManager;

class GeofenceRepositoryIntegrationTests extends PostgresIntegrationTest {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	@Autowired
	private GeofenceRepository geofenceRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@Transactional
	void persistsAndReadsPolygonWithSrid4326() {
		Instant validFrom = Instant.parse("2026-06-28T00:00:00Z");
		Instant validUntil = Instant.parse("2026-06-29T00:00:00Z");
		Polygon boundary = GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(106.6900, 10.7700),
				new Coordinate(106.7000, 10.7700),
				new Coordinate(106.7000, 10.7800),
				new Coordinate(106.6900, 10.7800),
				new Coordinate(106.6900, 10.7700)
		});

		Geofence saved = geofenceRepository.saveAndFlush(new Geofence(
				"SGN-DEMO-ZONE",
				"Demo restricted area",
				boundary,
				new BigDecimal("20.00"),
				new BigDecimal("120.00"),
				true,
				validFrom,
				validUntil));
		entityManager.clear();

		Geofence reloaded = geofenceRepository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getName()).isEqualTo("SGN-DEMO-ZONE");
		assertThat(reloaded.getBoundary().getSRID()).isEqualTo(4326);
		assertThat(reloaded.getBoundary().equalsExact(boundary)).isTrue();
		assertThat(reloaded.getMinAltitudeM()).isEqualByComparingTo("20.00");
		assertThat(reloaded.getMaxAltitudeM()).isEqualByComparingTo("120.00");
		assertThat(reloaded.isActive()).isTrue();
		assertThat(reloaded.getValidFrom()).isEqualTo(validFrom);
		assertThat(reloaded.getValidUntil()).isEqualTo(validUntil);
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getUpdatedAt()).isNotNull();
	}
}
