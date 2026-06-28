package com.portfolio.mini_utm.telemetry.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record TelemetryPageResponse(
		List<TelemetryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

	public static TelemetryPageResponse from(Page<TelemetryResponse> result) {
		return new TelemetryPageResponse(
				result.getContent(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.isFirst(),
				result.isLast());
	}
}
