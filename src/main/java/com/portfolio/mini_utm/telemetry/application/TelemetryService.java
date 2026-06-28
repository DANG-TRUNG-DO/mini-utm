package com.portfolio.mini_utm.telemetry.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.drone.application.DroneNotFoundException;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.mission.application.MissionNotFoundException;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.repository.MissionRepository;
import com.portfolio.mini_utm.telemetry.api.dto.IngestTelemetryRequest;
import com.portfolio.mini_utm.telemetry.api.dto.TelemetryPageResponse;
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

	@Transactional(readOnly = true)
	public TelemetryPageResponse findHistory(
			UUID droneId,
			Instant fromTime,
			Instant toTime,
			int page,
			int size) {
		if (!droneRepository.existsById(droneId)) {
			throw new DroneNotFoundException(droneId);
		}
		if (fromTime != null && toTime != null && !fromTime.isBefore(toTime)) {
			throw new InvalidTelemetryException("from must be before to");
		}
		if (page < 0) {
			throw new InvalidTelemetryException("page must be greater than or equal to 0");
		}
		if (size < 1 || size > 200) {
			throw new InvalidTelemetryException("size must be between 1 and 200");
		}

		PageRequest pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.desc("id")));
		Page<Telemetry> history = findHistory(droneId, fromTime, toTime, pageable);
		return TelemetryPageResponse.from(history.map(TelemetryResponse::from));
	}

	private Page<Telemetry> findHistory(
			UUID droneId,
			Instant fromTime,
			Instant toTime,
			PageRequest pageable) {
		if (fromTime != null && toTime != null) {
			return telemetryRepository
					.findByDroneIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(
							droneId, fromTime, toTime, pageable);
		}
		if (fromTime != null) {
			return telemetryRepository.findByDroneIdAndRecordedAtGreaterThanEqual(
					droneId, fromTime, pageable);
		}
		if (toTime != null) {
			return telemetryRepository.findByDroneIdAndRecordedAtLessThan(
					droneId, toTime, pageable);
		}
		return telemetryRepository.findByDroneId(droneId, pageable);
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
