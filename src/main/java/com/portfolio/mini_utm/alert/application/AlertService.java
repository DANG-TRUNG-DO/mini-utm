package com.portfolio.mini_utm.alert.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.alert.api.dto.AlertPageResponse;
import com.portfolio.mini_utm.alert.api.dto.AlertResponse;
import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.application.DroneNotFoundException;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.geofence.application.GeofenceNotFoundException;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.mission.application.MissionNotFoundException;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class AlertService {

	private final AlertRepository alertRepository;
	private final DroneRepository droneRepository;
	private final MissionRepository missionRepository;
	private final GeofenceRepository geofenceRepository;

	public AlertService(
			AlertRepository alertRepository,
			DroneRepository droneRepository,
			MissionRepository missionRepository,
			GeofenceRepository geofenceRepository) {
		this.alertRepository = alertRepository;
		this.droneRepository = droneRepository;
		this.missionRepository = missionRepository;
		this.geofenceRepository = geofenceRepository;
	}

	@Transactional
	public RaiseAlertResult raiseOrRefresh(RaiseAlertCommand command) {
		String dedupKey = normalizeDedupKey(command.dedupKey());
		Drone drone = droneRepository.findByIdForUpdate(command.droneId())
				.orElseThrow(() -> new DroneNotFoundException(command.droneId()));
		Mission mission = resolveMission(command, drone);
		Geofence geofence = resolveGeofence(command);

		return alertRepository.findByDroneIdAndTypeAndDedupKeyAndStatusNot(
				drone.getId(), command.type(), dedupKey, AlertStatus.RESOLVED)
				.map(existing -> refresh(existing, command))
				.orElseGet(() -> create(drone, mission, geofence, command, dedupKey));
	}

	@Transactional(readOnly = true)
	public AlertPageResponse findAll(
			UUID droneId,
			UUID missionId,
			AlertType type,
			AlertSeverity severity,
			AlertStatus status,
			int page,
			int size) {
		validatePage(page, size);
		PageRequest pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.desc("detectedAt"), Sort.Order.desc("id")));
		Page<AlertResponse> result = alertRepository.findAll(
				matching(droneId, missionId, type, severity, status), pageable)
				.map(AlertResponse::from);
		return AlertPageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public AlertResponse findById(UUID id) {
		return AlertResponse.from(alertRepository.findDetailsById(id)
				.orElseThrow(() -> new AlertNotFoundException(id)));
	}

	@Transactional
	public AlertResponse acknowledge(UUID id) {
		Alert alert = findByIdForUpdate(id);
		alert.acknowledge(Instant.now());
		return AlertResponse.from(alert);
	}

	@Transactional
	public AlertResponse resolve(UUID id) {
		Alert alert = findByIdForUpdate(id);
		alert.resolve(Instant.now());
		return AlertResponse.from(alert);
	}

	private Alert findByIdForUpdate(UUID id) {
		return alertRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new AlertNotFoundException(id));
	}

	private Specification<Alert> matching(
			UUID droneId,
			UUID missionId,
			AlertType type,
			AlertSeverity severity,
			AlertStatus status) {
		return (root, query, criteriaBuilder) -> {
			var predicates = new ArrayList<Predicate>();
			if (droneId != null) {
				predicates.add(criteriaBuilder.equal(root.get("drone").get("id"), droneId));
			}
			if (missionId != null) {
				predicates.add(criteriaBuilder.equal(root.get("mission").get("id"), missionId));
			}
			if (type != null) {
				predicates.add(criteriaBuilder.equal(root.get("type"), type));
			}
			if (severity != null) {
				predicates.add(criteriaBuilder.equal(root.get("severity"), severity));
			}
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private void validatePage(int page, int size) {
		if (page < 0) {
			throw new InvalidAlertException("page must be greater than or equal to 0");
		}
		if (size < 1 || size > 200) {
			throw new InvalidAlertException("size must be between 1 and 200");
		}
	}

	private RaiseAlertResult refresh(Alert alert, RaiseAlertCommand command) {
		alert.refresh(command.severity(), command.message(), command.detectedAt());
		return new RaiseAlertResult(alert, false);
	}

	private RaiseAlertResult create(
			Drone drone,
			Mission mission,
			Geofence geofence,
			RaiseAlertCommand command,
			String dedupKey) {
		Alert alert = new Alert(
				drone,
				mission,
				geofence,
				command.type(),
				command.severity(),
				dedupKey,
				command.message(),
				command.detectedAt());
		return new RaiseAlertResult(alertRepository.save(alert), true);
	}

	private Mission resolveMission(RaiseAlertCommand command, Drone drone) {
		if (command.missionId() == null) {
			return null;
		}
		Mission mission = missionRepository.findById(command.missionId())
				.orElseThrow(() -> new MissionNotFoundException(command.missionId()));
		if (!mission.getDrone().getId().equals(drone.getId())) {
			throw new InvalidAlertException("Mission does not belong to the specified drone");
		}
		return mission;
	}

	private Geofence resolveGeofence(RaiseAlertCommand command) {
		if (command.geofenceId() == null) {
			return null;
		}
		return geofenceRepository.findById(command.geofenceId())
				.orElseThrow(() -> new GeofenceNotFoundException(command.geofenceId()));
	}

	private String normalizeDedupKey(String dedupKey) {
		if (dedupKey == null || dedupKey.isBlank()) {
			throw new InvalidAlertException("dedupKey must not be blank");
		}
		String normalized = dedupKey.trim().toLowerCase(Locale.ROOT);
		if (normalized.length() > 200) {
			throw new InvalidAlertException("dedupKey must not exceed 200 characters");
		}
		return normalized;
	}
}
