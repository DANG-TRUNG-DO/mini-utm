package com.portfolio.mini_utm.drone.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.mini_utm.drone.domain.Drone;

import jakarta.persistence.LockModeType;

public interface DroneRepository extends JpaRepository<Drone, UUID> {

	boolean existsBySerialNumber(String serialNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM Drone d WHERE d.id = :id")
	Optional<Drone> findByIdForUpdate(@Param("id") UUID id);
}
