package com.portfolio.mini_utm.mission.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.mini_utm.mission.domain.Mission;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

	boolean existsByDroneIdAndNameIgnoreCase(UUID droneId, String name);

	@Query(value = """
			SELECT ST_Distance(
				ST_Force2D(planned_path)::geography,
				ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)
			FROM missions
			WHERE id = :missionId
			  AND drone_id = :droneId
			  AND status = 'ACTIVE'
			""", nativeQuery = true)
	Optional<Double> findHorizontalDistanceMeters(
			@Param("missionId") UUID missionId,
			@Param("droneId") UUID droneId,
			@Param("longitude") double longitude,
			@Param("latitude") double latitude);

	@EntityGraph(attributePaths = {"drone", "waypoints"})
	@Query("SELECT m FROM Mission m WHERE m.id = :id")
	Optional<Mission> findDetailsById(@Param("id") UUID id);
}
