package com.portfolio.mini_utm.drone.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;

import jakarta.persistence.LockModeType;

public interface DroneRepository extends JpaRepository<Drone, UUID> {

	boolean existsBySerialNumber(String serialNumber);

	@Query("""
			SELECT d.id AS droneId,
			       d.serialNumber AS serialNumber,
			       d.updatedAt AS monitoringStartedAt,
			       MAX(t.recordedAt) AS lastTelemetryAt
			FROM Drone d
			LEFT JOIN Telemetry t ON t.drone = d
			WHERE d.status = :status
			GROUP BY d.id, d.serialNumber, d.updatedAt
			""")
	List<DroneTelemetryActivity> findTelemetryActivityByStatus(
			@Param("status") DroneStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM Drone d WHERE d.id = :id")
	Optional<Drone> findByIdForUpdate(@Param("id") UUID id);
}
