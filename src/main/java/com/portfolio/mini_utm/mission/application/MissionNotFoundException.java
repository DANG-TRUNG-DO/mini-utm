package com.portfolio.mini_utm.mission.application;

import java.util.UUID;

public class MissionNotFoundException extends RuntimeException {

	public MissionNotFoundException(UUID id) {
		super("Mission not found: " + id);
	}
}
