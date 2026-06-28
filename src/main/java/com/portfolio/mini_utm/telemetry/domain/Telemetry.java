package com.portfolio.mini_utm.telemetry.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.locationtech.jts.geom.Point;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.mission.domain.Mission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "telemetry")
public class Telemetry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "drone_id", nullable = false)
	private Drone drone;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id")
	private Mission mission;

	@Column(name = "recorded_at", nullable = false)
	private Instant recordedAt;

	@Column(nullable = false, columnDefinition = "geometry(PointZ,4326)")
	private Point position;

	@Column(name = "speed_mps", precision = 10, scale = 3)
	private BigDecimal speedMps;

	@Column(name = "heading_degrees", precision = 6, scale = 2)
	private BigDecimal headingDegrees;

	@Column(name = "battery_percent", precision = 5, scale = 2)
	private BigDecimal batteryPercent;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Telemetry() {
	}

	public Telemetry(
			Drone drone,
			Mission mission,
			Instant recordedAt,
			Point position,
			BigDecimal speedMps,
			BigDecimal headingDegrees,
			BigDecimal batteryPercent) {
		this.drone = drone;
		this.mission = mission;
		this.recordedAt = recordedAt;
		this.position = position;
		this.speedMps = speedMps;
		this.headingDegrees = headingDegrees;
		this.batteryPercent = batteryPercent;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Drone getDrone() {
		return drone;
	}

	public Mission getMission() {
		return mission;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

	public Point getPosition() {
		return position;
	}

	public BigDecimal getSpeedMps() {
		return speedMps;
	}

	public BigDecimal getHeadingDegrees() {
		return headingDegrees;
	}

	public BigDecimal getBatteryPercent() {
		return batteryPercent;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
