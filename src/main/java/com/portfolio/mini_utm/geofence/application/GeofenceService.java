package com.portfolio.mini_utm.geofence.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.mini_utm.geofence.api.dto.CreateGeofenceRequest;
import com.portfolio.mini_utm.geofence.api.dto.GeofenceResponse;
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
		validateValidityPeriod(request);

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
		return toResponse(geofenceRepository.findById(id)
				.orElseThrow(() -> new GeofenceNotFoundException(id)));
	}

	private void validateAltitude(BigDecimal minimum, BigDecimal maximum) {
		if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
			throw new InvalidGeofenceException("Minimum altitude must not exceed maximum altitude");
		}
	}

	private void validateValidityPeriod(CreateGeofenceRequest request) {
		if (request.validFrom() != null && request.validUntil() != null
				&& !request.validFrom().isBefore(request.validUntil())) {
			throw new InvalidGeofenceException("validFrom must be before validUntil");
		}
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private GeofenceResponse toResponse(Geofence geofence) {
		return GeofenceResponse.from(geofence, polygonMapper.toGeoJson(geofence.getBoundary()));
	}
}
