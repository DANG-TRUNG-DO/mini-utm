package com.portfolio.mini_utm.alert.application;

import java.util.UUID;

public class AlertNotFoundException extends RuntimeException {

	public AlertNotFoundException(UUID id) {
		super("Alert not found: " + id);
	}
}
