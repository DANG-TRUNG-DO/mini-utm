package com.portfolio.mini_utm.geofence.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.locationtech.jts.geom.Polygon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "geofences")
public class Geofence {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 150)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(nullable = false, columnDefinition = "geometry(Polygon,4326)")
	private Polygon boundary;

	@Column(name = "min_altitude_m", precision = 10, scale = 2)
	private BigDecimal minAltitudeM;

	@Column(name = "max_altitude_m", precision = 10, scale = 2)
	private BigDecimal maxAltitudeM;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "valid_from")
	private Instant validFrom;

	@Column(name = "valid_until")
	private Instant validUntil;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Geofence() {
	}

	public Geofence(
			String name,
			String description,
			Polygon boundary,
			BigDecimal minAltitudeM,
			BigDecimal maxAltitudeM,
			boolean active,
			Instant validFrom,
			Instant validUntil) {
		this.name = name;
		this.description = description;
		this.boundary = boundary;
		this.minAltitudeM = minAltitudeM;
		this.maxAltitudeM = maxAltitudeM;
		this.active = active;
		this.validFrom = validFrom;
		this.validUntil = validUntil;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Polygon getBoundary() {
		return boundary;
	}

	public BigDecimal getMinAltitudeM() {
		return minAltitudeM;
	}

	public BigDecimal getMaxAltitudeM() {
		return maxAltitudeM;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getValidFrom() {
		return validFrom;
	}

	public Instant getValidUntil() {
		return validUntil;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
