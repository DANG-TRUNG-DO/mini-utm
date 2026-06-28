package com.portfolio.mini_utm.drone.domain;

public class InvalidDroneStatusTransitionException extends RuntimeException {

	public InvalidDroneStatusTransitionException(DroneStatus current, DroneStatus target) {
		super("Cannot transition drone status from %s to %s".formatted(current, target));
	}
}
