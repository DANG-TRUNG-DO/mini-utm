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

import static com.portfolio.mini_utm.config.ApiPaths.TELEMETRY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = TELEMETRY, produces = APPLICATION_JSON_VALUE)
public class TelemetryController {

	private final TelemetryService telemetryService;

	public TelemetryController(TelemetryService telemetryService) {
		this.telemetryService = telemetryService;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<TelemetryResponse> ingest(@Valid @RequestBody IngestTelemetryRequest request) {
		TelemetryResponse response = telemetryService.ingest(request);
		return ResponseEntity.created(URI.create(TELEMETRY + "/" + response.id())).body(response);
	}
}
