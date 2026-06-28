package com.portfolio.mini_utm.mission.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.drone.application.DroneNotFoundException;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;
import com.portfolio.mini_utm.mission.api.dto.CreateMissionRequest;
import com.portfolio.mini_utm.mission.api.dto.MissionResponse;
import com.portfolio.mini_utm.mission.api.dto.MissionWaypointRequest;
import com.portfolio.mini_utm.mission.domain.Mission;
import com.portfolio.mini_utm.mission.domain.MissionWaypoint;
import com.portfolio.mini_utm.mission.repository.MissionRepository;

@Service
public class MissionService {

	private final MissionRepository missionRepository;
	private final DroneRepository droneRepository;
	private final GeofenceRepository geofenceRepository;
	private final MissionGeometryFactory geometryFactory;

	public MissionService(
			MissionRepository missionRepository,
			DroneRepository droneRepository,
			GeofenceRepository geofenceRepository,
			MissionGeometryFactory geometryFactory) {
		this.missionRepository = missionRepository;
		this.droneRepository = droneRepository;
		this.geofenceRepository = geofenceRepository;
		this.geometryFactory = geometryFactory;
	}

	@Transactional
	public MissionResponse create(CreateMissionRequest request) {
		Drone drone = droneRepository.findById(request.droneId())
				.orElseThrow(() -> new DroneNotFoundException(request.droneId()));
		ensureDroneActive(drone);
		validateTimeRange(request.plannedStartAt(), request.plannedEndAt());

		String name = request.name().trim();
		if (missionRepository.existsByDroneIdAndNameIgnoreCase(drone.getId(), name)) {
			throw new DuplicateMissionNameException(name);
		}
		validateWaypointsAgainstGeofences(
				request.waypoints(), request.plannedStartAt(), request.plannedEndAt());

		Mission mission = new Mission(
				drone,
				name,
				geometryFactory.createPath(request.waypoints()),
				request.plannedStartAt(),
				request.plannedEndAt());
		for (int index = 0; index < request.waypoints().size(); index++) {
			mission.addWaypoint(new MissionWaypoint(
					index, geometryFactory.createPoint(request.waypoints().get(index))));
		}

		try {
			return MissionResponse.from(missionRepository.saveAndFlush(mission));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateMissionNameException(name);
		}
	}

	@Transactional(readOnly = true)
	public List<MissionResponse> findAll() {
		return missionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
				.map(MissionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public MissionResponse findById(UUID id) {
		return MissionResponse.from(getMission(id));
	}

	@Transactional
	public MissionResponse approve(UUID id) {
		Mission mission = getMission(id);
		ensureDroneActive(mission.getDrone());
		validatePersistedWaypointsAgainstGeofences(mission);
		mission.approve(Instant.now());
		return MissionResponse.from(missionRepository.saveAndFlush(mission));
	}

	@Transactional
	public MissionResponse start(UUID id) {
		Mission mission = getMission(id);
		ensureDroneActive(mission.getDrone());
		mission.start(Instant.now());
		return MissionResponse.from(missionRepository.saveAndFlush(mission));
	}

	@Transactional
	public MissionResponse complete(UUID id) {
		Mission mission = getMission(id);
		mission.complete(Instant.now());
		return MissionResponse.from(missionRepository.saveAndFlush(mission));
	}

	@Transactional
	public MissionResponse cancel(UUID id) {
		Mission mission = getMission(id);
		mission.cancel();
		return MissionResponse.from(missionRepository.saveAndFlush(mission));
	}

	private Mission getMission(UUID id) {
		return missionRepository.findDetailsById(id)
				.orElseThrow(() -> new MissionNotFoundException(id));
	}

	private void ensureDroneActive(Drone drone) {
		if (drone.getStatus() != DroneStatus.ACTIVE) {
			throw new InvalidMissionException(
					"Drone must be ACTIVE to plan or operate a mission; current status is " + drone.getStatus());
		}
	}

	private void validateTimeRange(Instant start, Instant end) {
		if (!start.isBefore(end)) {
			throw new InvalidMissionException("plannedStartAt must be before plannedEndAt");
		}
	}

	private void validateWaypointsAgainstGeofences(
			List<MissionWaypointRequest> waypoints,
			Instant plannedStartAt,
			Instant plannedEndAt) {
		for (int index = 0; index < waypoints.size(); index++) {
			MissionWaypointRequest waypoint = waypoints.get(index);
			List<Geofence> restrictions = geofenceRepository.findRestrictionsCoveringDuring(
					waypoint.longitude(),
					waypoint.latitude(),
					waypoint.altitudeM(),
					plannedStartAt,
					plannedEndAt);
			throwIfRestricted(index, restrictions);
		}
	}

	private void validatePersistedWaypointsAgainstGeofences(Mission mission) {
		for (MissionWaypoint waypoint : mission.getWaypoints()) {
			List<Geofence> restrictions = geofenceRepository.findRestrictionsCoveringDuring(
					waypoint.getPosition().getX(),
					waypoint.getPosition().getY(),
					BigDecimal.valueOf(waypoint.getPosition().getCoordinate().getZ()),
					mission.getPlannedStartAt(),
					mission.getPlannedEndAt());
			throwIfRestricted(waypoint.getSequenceNumber(), restrictions);
		}
	}

	private void throwIfRestricted(int waypointIndex, List<Geofence> restrictions) {
		if (!restrictions.isEmpty()) {
			String names = restrictions.stream().map(Geofence::getName).collect(Collectors.joining(", "));
			throw new InvalidMissionException(
					"Waypoint %d intersects active geofence(s): %s".formatted(waypointIndex, names));
		}
	}
}
