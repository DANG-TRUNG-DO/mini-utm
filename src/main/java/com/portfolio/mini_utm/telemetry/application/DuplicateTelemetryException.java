package com.portfolio.mini_utm.telemetry.application;

import java.time.Instant;
import java.util.UUID;

public class DuplicateTelemetryException extends RuntimeException {

	public DuplicateTelemetryException(UUID droneId, Instant recordedAt) {
		super("Telemetry already exists for drone %s at %s".formatted(droneId, recordedAt));
	}
}
