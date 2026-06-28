package com.portfolio.mini_utm.geofence.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record GeoJsonPolygon(
		@NotBlank String type,
		@NotEmpty List<List<List<Double>>> coordinates) {
}
