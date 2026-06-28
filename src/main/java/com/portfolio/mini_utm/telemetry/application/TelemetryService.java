package com.portfolio.mini_utm.telemetry.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.drone.application.DroneNotFoundException;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.mission.application.MissionNotFoundException;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;
import com.portfolio.mini_utm.telemetry.api.dto.IngestTelemetryRequest;
import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;
import com.portfolio.mini_utm.telemetry.domain.Telemetry;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

@Service
public class TelemetryService {

	private final TelemetryRepository telemetryRepository;
	private final DroneRepository droneRepository;
	private final MissionRepository missionRepository;
	private final TelemetryGeometryFactory geometryFactory;

	public TelemetryService(
			TelemetryRepository telemetryRepository,
			DroneRepository droneRepository,
			MissionRepository missionRepository,
			TelemetryGeometryFactory geometryFactory) {
		this.telemetryRepository = telemetryRepository;
		this.droneRepository = droneRepository;
		this.missionRepository = missionRepository;
		this.geometryFactory = geometryFactory;
	}

	@Transactional
	public TelemetryResponse ingest(IngestTelemetryRequest request) {
		Drone drone = droneRepository.findById(request.droneId())
				.orElseThrow(() -> new DroneNotFoundException(request.droneId()));
		Mission mission = resolveMission(request, drone);

		if (telemetryRepository.existsByDroneIdAndRecordedAt(drone.getId(), request.recordedAt())) {
			throw new DuplicateTelemetryException(drone.getId(), request.recordedAt());
		}

		Telemetry telemetry = new Telemetry(
				drone,
				mission,
				request.recordedAt(),
				geometryFactory.createPosition(request),
				request.speedMps(),
				request.headingDegrees(),
				request.batteryPercent());
		try {
			return TelemetryResponse.from(telemetryRepository.saveAndFlush(telemetry));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateTelemetryException(drone.getId(), request.recordedAt());
		}
	}

	private Mission resolveMission(IngestTelemetryRequest request, Drone drone) {
		if (request.missionId() == null) {
			return null;
		}
		Mission mission = missionRepository.findById(request.missionId())
				.orElseThrow(() -> new MissionNotFoundException(request.missionId()));
		if (!mission.getDrone().getId().equals(drone.getId())) {
			throw new InvalidTelemetryException("Mission does not belong to the specified drone");
		}
		return mission;
	}
}
