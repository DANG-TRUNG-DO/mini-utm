package com.portfolio.mini_utm.mission.api.dto;

import java.math.BigDecimal;

import com.portfolio.mini_utm.mission.domain.MissionWaypoint;

public record MissionWaypointResponse(
		int sequenceNumber,
		double longitude,
		double latitude,
		BigDecimal altitudeM) {

	public static MissionWaypointResponse from(MissionWaypoint waypoint) {
		return new MissionWaypointResponse(
				waypoint.getSequenceNumber(),
				waypoint.getPosition().getX(),
				waypoint.getPosition().getY(),
				BigDecimal.valueOf(waypoint.getPosition().getCoordinate().getZ()));
	}
}
