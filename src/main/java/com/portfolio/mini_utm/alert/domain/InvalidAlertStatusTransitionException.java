package com.portfolio.mini_utm.alert.domain;

public class InvalidAlertStatusTransitionException extends RuntimeException {

	public InvalidAlertStatusTransitionException(AlertStatus current, AlertStatus target) {
		super("Cannot transition alert status from %s to %s".formatted(current, target));
	}
}
