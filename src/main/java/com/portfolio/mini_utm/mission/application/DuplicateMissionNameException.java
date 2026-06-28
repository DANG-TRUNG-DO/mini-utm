package com.portfolio.mini_utm.mission.application;

public class DuplicateMissionNameException extends RuntimeException {

	public DuplicateMissionNameException(String name) {
		super("Mission name already exists for this drone: " + name);
	}
}
