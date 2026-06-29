package com.portfolio.mini_utm.drone.repository;

import java.time.Instant;
import java.util.UUID;

public interface DroneTelemetryActivity {

	UUID getDroneId();

	String getSerialNumber();

	Instant getMonitoringStartedAt();

	Instant getLastTelemetryAt();
}
