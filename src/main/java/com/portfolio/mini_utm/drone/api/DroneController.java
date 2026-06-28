package com.portfolio.mini_utm.drone.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.mini_utm.drone.api.dto.DroneResponse;
import com.portfolio.mini_utm.drone.api.dto.RegisterDroneRequest;
import com.portfolio.mini_utm.drone.api.dto.UpdateDroneRequest;
import com.portfolio.mini_utm.drone.api.dto.UpdateDroneStatusRequest;
import com.portfolio.mini_utm.drone.application.DroneService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/drones")
public class DroneController {

	private final DroneService droneService;

	public DroneController(DroneService droneService) {
		this.droneService = droneService;
	}

	@PostMapping
	public ResponseEntity<DroneResponse> register(@Valid @RequestBody RegisterDroneRequest request) {
		DroneResponse response = droneService.register(request);
		return ResponseEntity.created(URI.create("/api/v1/drones/" + response.id())).body(response);
	}

	@GetMapping
	public List<DroneResponse> findAll() {
		return droneService.findAll();
	}

	@GetMapping("/{id}")
	public DroneResponse findById(@PathVariable UUID id) {
		return droneService.findById(id);
	}

	@PatchMapping("/{id}")
	public DroneResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDroneRequest request) {
		return droneService.update(id, request);
	}

	@PatchMapping("/{id}/status")
	public DroneResponse updateStatus(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateDroneStatusRequest request) {
		return droneService.updateStatus(id, request.status());
	}
}
