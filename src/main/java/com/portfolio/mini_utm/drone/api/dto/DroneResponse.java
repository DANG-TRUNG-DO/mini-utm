package com.portfolio.mini_utm.drone.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;

public record DroneResponse(
		UUID id,
		String serialNumber,
		String name,
		DroneStatus status,
		String model,
		Instant registeredAt,
		Instant updatedAt) {

	public static DroneResponse from(Drone drone) {
		return new DroneResponse(
				drone.getId(),
				drone.getSerialNumber(),
				drone.getName(),
				drone.getStatus(),
				drone.getModel(),
				drone.getRegisteredAt(),
				drone.getUpdatedAt());
	}
}
