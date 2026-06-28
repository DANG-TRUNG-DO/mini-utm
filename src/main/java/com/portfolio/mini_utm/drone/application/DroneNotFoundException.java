package com.portfolio.mini_utm.drone.application;

import java.util.UUID;

public class DroneNotFoundException extends RuntimeException {

	public DroneNotFoundException(UUID id) {
		super("Drone not found: " + id);
	}
}
