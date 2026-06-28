package com.portfolio.mini_utm.drone.application;

public class DuplicateDroneSerialNumberException extends RuntimeException {

	public DuplicateDroneSerialNumberException(String serialNumber) {
		super("Drone serial number already exists: " + serialNumber);
	}
}
