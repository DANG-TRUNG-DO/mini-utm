package com.portfolio.mini_utm.mission.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.mini_utm.mission.domain.Mission;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

	boolean existsByDroneIdAndNameIgnoreCase(UUID droneId, String name);

	@EntityGraph(attributePaths = {"drone", "waypoints"})
	@Query("SELECT m FROM Mission m WHERE m.id = :id")
	java.util.Optional<Mission> findDetailsById(@Param("id") UUID id);
}
