package com.portfolio.mini_utm.telemetry.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.mini_utm.telemetry.domain.Telemetry;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {

	boolean existsByDroneIdAndRecordedAt(UUID droneId, Instant recordedAt);
}
