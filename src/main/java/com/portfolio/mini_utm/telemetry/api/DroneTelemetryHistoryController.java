package com.portfolio.mini_utm.telemetry.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.mini_utm.telemetry.api.dto.TelemetryPageResponse;
import com.portfolio.mini_utm.telemetry.application.TelemetryService;

@RestController
@RequestMapping("/api/v1/drones/{droneId}/telemetry")
public class DroneTelemetryHistoryController {

	private final TelemetryService telemetryService;

	public DroneTelemetryHistoryController(TelemetryService telemetryService) {
		this.telemetryService = telemetryService;
	}

	@GetMapping
	public TelemetryPageResponse findHistory(
			@PathVariable UUID droneId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return telemetryService.findHistory(droneId, from, to, page, size);
	}
}
