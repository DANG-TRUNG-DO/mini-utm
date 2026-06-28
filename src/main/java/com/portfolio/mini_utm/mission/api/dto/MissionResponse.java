package com.portfolio.mini_utm.mission.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.domain.MissionStatus;

public record MissionResponse(
		UUID id,
		UUID droneId,
		String name,
		MissionStatus status,
		Instant plannedStartAt,
		Instant plannedEndAt,
		Instant approvedAt,
		Instant startedAt,
		Instant completedAt,
		Instant createdAt,
		Instant updatedAt,
		List<MissionWaypointResponse> waypoints) {

	public static MissionResponse from(Mission mission) {
		return new MissionResponse(
				mission.getId(),
				mission.getDrone().getId(),
				mission.getName(),
				mission.getStatus(),
				mission.getPlannedStartAt(),
				mission.getPlannedEndAt(),
				mission.getApprovedAt(),
				mission.getStartedAt(),
				mission.getCompletedAt(),
				mission.getCreatedAt(),
				mission.getUpdatedAt(),
				mission.getWaypoints().stream().map(MissionWaypointResponse::from).toList());
	}
}
