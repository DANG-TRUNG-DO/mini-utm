package com.portfolio.mini_utm.mission.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMissionRequest(
		@NotNull UUID droneId,
		@NotBlank @Size(max = 150) String name,
		@NotNull Instant plannedStartAt,
		@NotNull Instant plannedEndAt,
		@NotNull @Size(min = 2, max = 500) List<@Valid MissionWaypointRequest> waypoints) {
}
