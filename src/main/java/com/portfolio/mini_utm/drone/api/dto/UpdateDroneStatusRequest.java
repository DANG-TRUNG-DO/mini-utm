package com.portfolio.mini_utm.drone.api.dto;

import com.portfolio.mini_utm.drone.domain.DroneStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateDroneStatusRequest(@NotNull DroneStatus status) {
}
