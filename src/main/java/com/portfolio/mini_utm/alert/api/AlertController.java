package com.portfolio.mini_utm.alert.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.mini_utm.alert.api.dto.AlertPageResponse;
import com.portfolio.mini_utm.alert.api.dto.AlertResponse;
import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

	private final AlertService alertService;

	public AlertController(AlertService alertService) {
		this.alertService = alertService;
	}

	@GetMapping
	public AlertPageResponse findAll(
			@RequestParam(required = false) UUID droneId,
			@RequestParam(required = false) UUID missionId,
			@RequestParam(required = false) AlertType type,
			@RequestParam(required = false) AlertSeverity severity,
			@RequestParam(required = false) AlertStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return alertService.findAll(
				droneId, missionId, type, severity, status, page, size);
	}

	@GetMapping("/{id}")
	public AlertResponse findById(@PathVariable UUID id) {
		return alertService.findById(id);
	}

	@PostMapping("/{id}/acknowledge")
	public AlertResponse acknowledge(@PathVariable UUID id) {
		return alertService.acknowledge(id);
	}

	@PostMapping("/{id}/resolve")
	public AlertResponse resolve(@PathVariable UUID id) {
		return alertService.resolve(id);
	}
}
