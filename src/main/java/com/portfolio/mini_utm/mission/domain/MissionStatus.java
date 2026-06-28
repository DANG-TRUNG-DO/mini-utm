package com.portfolio.mini_utm.mission.domain;

public enum MissionStatus {
	PLANNED,
	APPROVED,
	ACTIVE,
	COMPLETED,
	CANCELLED;

	public boolean canTransitionTo(MissionStatus target) {
		if (target == null || target == this) {
			return false;
		}

		return switch (this) {
			case PLANNED -> target == APPROVED || target == CANCELLED;
			case APPROVED -> target == ACTIVE || target == CANCELLED;
			case ACTIVE -> target == COMPLETED || target == CANCELLED;
			case COMPLETED, CANCELLED -> false;
		};
	}
}
