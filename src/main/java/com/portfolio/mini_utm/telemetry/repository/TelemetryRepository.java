package com.portfolio.mini_utm.telemetry.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.mini_utm.telemetry.domain.Telemetry;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {

	boolean existsByDroneIdAndRecordedAt(UUID droneId, Instant recordedAt);

	@EntityGraph(attributePaths = {"drone", "mission"})
	Page<Telemetry> findByDroneId(UUID droneId, Pageable pageable);

	@EntityGraph(attributePaths = {"drone", "mission"})
	Page<Telemetry> findByDroneIdAndRecordedAtGreaterThanEqual(
			UUID droneId,
			Instant fromTime,
			Pageable pageable);

	@EntityGraph(attributePaths = {"drone", "mission"})
	Page<Telemetry> findByDroneIdAndRecordedAtLessThan(
			UUID droneId,
			Instant toTime,
			Pageable pageable);

	@EntityGraph(attributePaths = {"drone", "mission"})
	Page<Telemetry> findByDroneIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
			UUID droneId,
			Instant fromTime,
			Instant toTime,
			Pageable pageable);
}
