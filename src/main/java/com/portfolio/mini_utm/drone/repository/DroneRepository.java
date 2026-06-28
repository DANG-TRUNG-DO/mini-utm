package com.portfolio.mini_utm.drone.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.mini_utm.drone.domain.Drone;

public interface DroneRepository extends JpaRepository<Drone, UUID> {

	boolean existsBySerialNumber(String serialNumber);
}
