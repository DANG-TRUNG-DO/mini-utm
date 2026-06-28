package com.portfolio.mini_utm.geofence.application;

public class DuplicateGeofenceNameException extends RuntimeException {

	public DuplicateGeofenceNameException(String name) {
		super("Geofence name already exists: " + name);
	}
}
