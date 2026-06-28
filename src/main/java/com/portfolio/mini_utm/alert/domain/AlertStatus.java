package com.portfolio.mini_utm.alert.domain;

public enum AlertStatus {
	OPEN,
	ACKNOWLEDGED,
	RESOLVED;

	public boolean canTransitionTo(AlertStatus target) {
		if (target == null || target == this) {
			return false;
		}

		return switch (this) {
			case OPEN -> target == ACKNOWLEDGED || target == RESOLVED;
			case ACKNOWLEDGED -> target == RESOLVED;
			case RESOLVED -> false;
		};
	}
}
