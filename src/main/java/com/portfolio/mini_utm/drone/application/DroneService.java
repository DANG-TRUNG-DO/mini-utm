package com.portfolio.mini_utm.drone.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.drone.api.dto.DroneResponse;
import com.portfolio.mini_utm.drone.api.dto.RegisterDroneRequest;
import com.portfolio.mini_utm.drone.api.dto.UpdateDroneRequest;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;

@Service
public class DroneService {

	private final DroneRepository droneRepository;

	public DroneService(DroneRepository droneRepository) {
		this.droneRepository = droneRepository;
	}

	@Transactional
	public DroneResponse register(RegisterDroneRequest request) {
		String serialNumber = normalizeSerialNumber(request.serialNumber());
		if (droneRepository.existsBySerialNumber(serialNumber)) {
			throw new DuplicateDroneSerialNumberException(serialNumber);
		}

		Drone drone = new Drone(serialNumber, request.name().trim(), normalizeOptional(request.model()));
		try {
			return DroneResponse.from(droneRepository.saveAndFlush(drone));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateDroneSerialNumberException(serialNumber);
		}
	}

	@Transactional(readOnly = true)
	public List<DroneResponse> findAll() {
		return droneRepository.findAll(Sort.by(Sort.Direction.DESC, "registeredAt")).stream()
				.map(DroneResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public DroneResponse findById(UUID id) {
		return DroneResponse.from(getDrone(id));
	}

	@Transactional
	public DroneResponse update(UUID id, UpdateDroneRequest request) {
		Drone drone = getDrone(id);
		String name = request.name() == null ? null : request.name().trim();
		drone.updateDetails(name, normalizeOptional(request.model()));
		return DroneResponse.from(droneRepository.saveAndFlush(drone));
	}

	@Transactional
	public DroneResponse updateStatus(UUID id, DroneStatus status) {
		Drone drone = getDrone(id);
		drone.transitionTo(status);
		return DroneResponse.from(droneRepository.saveAndFlush(drone));
	}

	private Drone getDrone(UUID id) {
		return droneRepository.findById(id).orElseThrow(() -> new DroneNotFoundException(id));
	}

	private String normalizeSerialNumber(String serialNumber) {
		return serialNumber.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeOptional(String value) {
		return value == null ? null : value.trim();
	}
}
