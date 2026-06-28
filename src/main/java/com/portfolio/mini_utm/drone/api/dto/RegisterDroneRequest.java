package com.portfolio.mini_utm.drone.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDroneRequest(
		@NotBlank @Size(max = 100) String serialNumber,
		@NotBlank @Size(max = 150) String name,
		@Size(max = 100) String model) {
}
