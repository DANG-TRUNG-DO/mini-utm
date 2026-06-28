package com.portfolio.mini_utm.geofence.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.mini_utm.geofence.api.dto.CreateGeofenceRequest;
import com.portfolio.mini_utm.geofence.api.dto.GeofenceResponse;
import com.portfolio.mini_utm.geofence.application.GeofenceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/geofences")
public class GeofenceController {

	private final GeofenceService geofenceService;

	public GeofenceController(GeofenceService geofenceService) {
		this.geofenceService = geofenceService;
	}

	@PostMapping
	public ResponseEntity<GeofenceResponse> create(@Valid @RequestBody CreateGeofenceRequest request) {
		GeofenceResponse response = geofenceService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/geofences/" + response.id())).body(response);
	}

	@GetMapping
	public List<GeofenceResponse> findAll() {
		return geofenceService.findAll();
	}

	@GetMapping("/{id}")
	public GeofenceResponse findById(@PathVariable UUID id) {
		return geofenceService.findById(id);
	}
}
