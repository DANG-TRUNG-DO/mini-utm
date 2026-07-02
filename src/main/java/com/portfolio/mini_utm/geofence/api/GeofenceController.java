package com.portfolio.mini_utm.geofence.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.mini_utm.geofence.api.dto.CreateGeofenceRequest;
import com.portfolio.mini_utm.geofence.api.dto.CheckGeofenceRequest;
import com.portfolio.mini_utm.geofence.api.dto.CheckGeofenceResponse;
import com.portfolio.mini_utm.geofence.api.dto.GeofenceResponse;
import com.portfolio.mini_utm.geofence.api.dto.UpdateGeofenceRequest;
import com.portfolio.mini_utm.geofence.application.GeofenceService;

import jakarta.validation.Valid;

import static com.portfolio.mini_utm.config.ApiPaths.GEOFENCES;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = GEOFENCES, produces = APPLICATION_JSON_VALUE)
public class GeofenceController {

	private final GeofenceService geofenceService;

	public GeofenceController(GeofenceService geofenceService) {
		this.geofenceService = geofenceService;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<GeofenceResponse> create(@Valid @RequestBody CreateGeofenceRequest request) {
		GeofenceResponse response = geofenceService.create(request);
		return ResponseEntity.created(URI.create(GEOFENCES + "/" + response.id())).body(response);
	}

	@GetMapping
	public List<GeofenceResponse> findAll() {
		return geofenceService.findAll();
	}

	@GetMapping("/{id}")
	public GeofenceResponse findById(@PathVariable UUID id) {
		return geofenceService.findById(id);
	}

	@PatchMapping(value = "/{id}", consumes = APPLICATION_JSON_VALUE)
	public GeofenceResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateGeofenceRequest request) {
		return geofenceService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		geofenceService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(value = "/check", consumes = APPLICATION_JSON_VALUE)
	public CheckGeofenceResponse check(@Valid @RequestBody CheckGeofenceRequest request) {
		return geofenceService.checkRestrictions(request);
	}
}
