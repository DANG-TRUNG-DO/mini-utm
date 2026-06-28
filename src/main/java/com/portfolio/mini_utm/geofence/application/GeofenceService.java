package com.portfolio.mini_utm.geofence.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.geofence.api.dto.CreateGeofenceRequest;
import com.portfolio.mini_utm.geofence.api.dto.CheckGeofenceRequest;
import com.portfolio.mini_utm.geofence.api.dto.CheckGeofenceResponse;
import com.portfolio.mini_utm.geofence.api.dto.GeofenceMatchResponse;
import com.portfolio.mini_utm.geofence.api.dto.GeofenceResponse;
import com.portfolio.mini_utm.geofence.api.dto.UpdateGeofenceRequest;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.geofence.repository.GeofenceRepository;

@Service
public class GeofenceService {

	private final GeofenceRepository geofenceRepository;
	private final GeoJsonPolygonMapper polygonMapper;

	public GeofenceService(GeofenceRepository geofenceRepository, GeoJsonPolygonMapper polygonMapper) {
		this.geofenceRepository = geofenceRepository;
		this.polygonMapper = polygonMapper;
	}

	@Transactional
	public GeofenceResponse create(CreateGeofenceRequest request) {
		String name = request.name().trim();
		if (geofenceRepository.existsByNameIgnoreCase(name)) {
			throw new DuplicateGeofenceNameException(name);
		}
		validateAltitude(request.minAltitudeM(), request.maxAltitudeM());
		validateValidityPeriod(request.validFrom(), request.validUntil());

		Geofence geofence = new Geofence(
				name,
				normalizeOptional(request.description()),
				polygonMapper.toPolygon(request.boundary()),
				request.minAltitudeM(),
				request.maxAltitudeM(),
				request.active(),
				request.validFrom(),
				request.validUntil());
		try {
			Geofence saved = geofenceRepository.saveAndFlush(geofence);
			return toResponse(saved);
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateGeofenceNameException(name);
		}
	}

	@Transactional(readOnly = true)
	public List<GeofenceResponse> findAll() {
		return geofenceRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public GeofenceResponse findById(UUID id) {
		return toResponse(getGeofence(id));
	}

	@Transactional
	public GeofenceResponse update(UUID id, UpdateGeofenceRequest request) {
		Geofence geofence = getGeofence(id);
		String name = request.name() == null ? geofence.getName() : request.name().trim();
		if (geofenceRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new DuplicateGeofenceNameException(name);
		}

		String description = request.description() == null
				? geofence.getDescription()
				: normalizeOptional(request.description());
		var boundary = request.boundary() == null
				? geofence.getBoundary()
				: polygonMapper.toPolygon(request.boundary());
		BigDecimal minimumAltitude = request.minAltitudeM() == null
				? geofence.getMinAltitudeM()
				: request.minAltitudeM();
		BigDecimal maximumAltitude = request.maxAltitudeM() == null
				? geofence.getMaxAltitudeM()
				: request.maxAltitudeM();
		boolean active = request.active() == null ? geofence.isActive() : request.active();
		var validFrom = request.validFrom() == null ? geofence.getValidFrom() : request.validFrom();
		var validUntil = request.validUntil() == null ? geofence.getValidUntil() : request.validUntil();

		validateAltitude(minimumAltitude, maximumAltitude);
		validateValidityPeriod(validFrom, validUntil);
		geofence.updateDetails(
				name,
				description,
				boundary,
				minimumAltitude,
				maximumAltitude,
				active,
				validFrom,
				validUntil);
		try {
			return toResponse(geofenceRepository.saveAndFlush(geofence));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateGeofenceNameException(name);
		}
	}

	@Transactional
	public void delete(UUID id) {
		Geofence geofence = getGeofence(id);
		geofenceRepository.delete(geofence);
		geofenceRepository.flush();
	}

	@Transactional(readOnly = true)
	public CheckGeofenceResponse checkRestrictions(CheckGeofenceRequest request) {
		if (!Double.isFinite(request.longitude()) || !Double.isFinite(request.latitude())) {
			throw new InvalidGeofenceException("Coordinates must be finite numbers");
		}

		List<GeofenceMatchResponse> matches = geofenceRepository.findRestrictionsCovering(
				request.longitude(),
				request.latitude(),
				request.altitudeM(),
				request.checkedAt()).stream()
				.map(GeofenceMatchResponse::from)
				.toList();
		return new CheckGeofenceResponse(!matches.isEmpty(), matches);
	}

	private void validateAltitude(BigDecimal minimum, BigDecimal maximum) {
		if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
			throw new InvalidGeofenceException("Minimum altitude must not exceed maximum altitude");
		}
	}

	private void validateValidityPeriod(java.time.Instant validFrom, java.time.Instant validUntil) {
		if (validFrom != null && validUntil != null && !validFrom.isBefore(validUntil)) {
			throw new InvalidGeofenceException("validFrom must be before validUntil");
		}
	}

	private Geofence getGeofence(UUID id) {
		return geofenceRepository.findById(id)
				.orElseThrow(() -> new GeofenceNotFoundException(id));
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private GeofenceResponse toResponse(Geofence geofence) {
		return GeofenceResponse.from(geofence, polygonMapper.toGeoJson(geofence.getBoundary()));
	}
}
