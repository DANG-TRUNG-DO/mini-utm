package com.portfolio.mini_utm.drone.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDroneRequest(
		@Pattern(regexp = ".*\\S.*", message = "must contain a non-whitespace character")
		@Size(max = 150) String name,
		@Size(max = 100) String model) {
}
