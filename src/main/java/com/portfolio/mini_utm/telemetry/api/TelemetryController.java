package com.portfolio.mini_utm.telemetry.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.mini_utm.telemetry.api.dto.IngestTelemetryRequest;
import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;
import com.portfolio.mini_utm.telemetry.application.TelemetryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

	private final TelemetryService telemetryService;

	public TelemetryController(TelemetryService telemetryService) {
		this.telemetryService = telemetryService;
	}

	@PostMapping
	public ResponseEntity<TelemetryResponse> ingest(@Valid @RequestBody IngestTelemetryRequest request) {
		TelemetryResponse response = telemetryService.ingest(request);
		return ResponseEntity.created(URI.create("/api/v1/telemetry/" + response.id())).body(response);
	}
}
