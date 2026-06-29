package com.portfolio.mini_utm.alert.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AlertPageResponse(
		List<AlertResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

	public static AlertPageResponse from(Page<AlertResponse> result) {
		return new AlertPageResponse(
				result.getContent(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.isFirst(),
				result.isLast());
	}
}
