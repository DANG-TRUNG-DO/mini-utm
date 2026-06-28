package com.portfolio.mini_utm.drone.domain;

public enum DroneStatus {
	ACTIVE,
	INACTIVE,
	MAINTENANCE;

	public boolean canTransitionTo(DroneStatus target) {
		if (target == null || target == this) {
			return false;
		}

		return switch (this) {
			case ACTIVE -> target == INACTIVE || target == MAINTENANCE;
			case INACTIVE -> target == ACTIVE || target == MAINTENANCE;
			case MAINTENANCE -> target == INACTIVE;
		};
	}
}
