package com.portfolio.mini_utm.mission.api;

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

import com.portfolio.mini_utm.mission.api.dto.CreateMissionRequest;
import com.portfolio.mini_utm.mission.api.dto.MissionResponse;
import com.portfolio.mini_utm.mission.application.MissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/missions")
public class MissionController {

	private final MissionService missionService;

	public MissionController(MissionService missionService) {
		this.missionService = missionService;
	}

	@PostMapping
	public ResponseEntity<MissionResponse> create(@Valid @RequestBody CreateMissionRequest request) {
		MissionResponse response = missionService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/missions/" + response.id())).body(response);
	}

	@GetMapping
	public List<MissionResponse> findAll() {
		return missionService.findAll();
	}

	@GetMapping("/{id}")
	public MissionResponse findById(@PathVariable UUID id) {
		return missionService.findById(id);
	}

	@PostMapping("/{id}/approve")
	public MissionResponse approve(@PathVariable UUID id) {
		return missionService.approve(id);
	}

	@PostMapping("/{id}/start")
	public MissionResponse start(@PathVariable UUID id) {
		return missionService.start(id);
	}

	@PostMapping("/{id}/complete")
	public MissionResponse complete(@PathVariable UUID id) {
		return missionService.complete(id);
	}

	@PostMapping("/{id}/cancel")
	public MissionResponse cancel(@PathVariable UUID id) {
		return missionService.cancel(id);
	}
}
