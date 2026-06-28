package com.portfolio.mini_utm.mission.domain;

public class InvalidMissionStatusTransitionException extends RuntimeException {

	public InvalidMissionStatusTransitionException(MissionStatus current, MissionStatus target) {
		super("Cannot transition mission status from %s to %s".formatted(current, target));
	}
}
