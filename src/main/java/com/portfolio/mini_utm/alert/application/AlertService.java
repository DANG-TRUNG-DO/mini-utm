package com.portfolio.mini_utm.alert.application;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
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
