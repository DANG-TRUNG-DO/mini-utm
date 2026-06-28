package com.portfolio.mini_utm.geofence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.mini_utm.geofence.domain.Geofence;

public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

	@Query(value = """
			SELECT g.*
			FROM geofences g
			WHERE g.active = TRUE
			  AND (g.valid_from IS NULL OR g.valid_from <= :checkedAt)
			  AND (g.valid_until IS NULL OR g.valid_until > :checkedAt)
			  AND (g.min_altitude_m IS NULL OR g.min_altitude_m <= :altitudeM)
			  AND (g.max_altitude_m IS NULL OR g.max_altitude_m >= :altitudeM)
			  AND ST_Covers(
			        g.boundary,
			        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
			      )
			ORDER BY g.name
			""", nativeQuery = true)
	List<Geofence> findRestrictionsCovering(
			@Param("longitude") double longitude,
			@Param("latitude") double latitude,
			@Param("altitudeM") BigDecimal altitudeM,
			@Param("checkedAt") Instant checkedAt);

	@Query(value = """
			SELECT g.*
			FROM geofences g
			WHERE g.active = TRUE
			  AND (g.valid_from IS NULL OR g.valid_from < :plannedEndAt)
			  AND (g.valid_until IS NULL OR g.valid_until > :plannedStartAt)
			  AND (g.min_altitude_m IS NULL OR g.min_altitude_m <= :altitudeM)
			  AND (g.max_altitude_m IS NULL OR g.max_altitude_m >= :altitudeM)
			  AND ST_Covers(
			        g.boundary,
			        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
			      )
			ORDER BY g.name
			""", nativeQuery = true)
	List<Geofence> findRestrictionsCoveringDuring(
			@Param("longitude") double longitude,
			@Param("latitude") double latitude,
			@Param("altitudeM") BigDecimal altitudeM,
			@Param("plannedStartAt") Instant plannedStartAt,
			@Param("plannedEndAt") Instant plannedEndAt);

	@Query(value = """
			SELECT g.*
			FROM geofences g
			WHERE g.active = TRUE
			  AND (g.valid_from IS NULL OR g.valid_from < :plannedEndAt)
			  AND (g.valid_until IS NULL OR g.valid_until > :plannedStartAt)
			  AND (g.min_altitude_m IS NULL OR g.min_altitude_m <= :segmentMaxAltitudeM)
			  AND (g.max_altitude_m IS NULL OR g.max_altitude_m >= :segmentMinAltitudeM)
			  AND ST_Intersects(
			        g.boundary,
			        ST_SetSRID(
			          ST_MakeLine(
			            ST_MakePoint(:startLongitude, :startLatitude),
			            ST_MakePoint(:endLongitude, :endLatitude)
			          ),
			          4326
			        )
			      )
			ORDER BY g.name
			""", nativeQuery = true)
	List<Geofence> findRestrictionsIntersectingSegmentDuring(
			@Param("startLongitude") double startLongitude,
			@Param("startLatitude") double startLatitude,
			@Param("endLongitude") double endLongitude,
			@Param("endLatitude") double endLatitude,
			@Param("segmentMinAltitudeM") BigDecimal segmentMinAltitudeM,
			@Param("segmentMaxAltitudeM") BigDecimal segmentMaxAltitudeM,
			@Param("plannedStartAt") Instant plannedStartAt,
			@Param("plannedEndAt") Instant plannedEndAt);
}
