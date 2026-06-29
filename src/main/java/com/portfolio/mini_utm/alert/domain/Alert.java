package com.portfolio.mini_utm.alert.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.geofence.domain.Geofence;
import com.portfolio.mini_utm.mission.domain.Mission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts")
public class Alert {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "drone_id", nullable = false)
	private Drone drone;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id")
	private Mission mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "geofence_id")
	private Geofence geofence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AlertType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AlertSeverity severity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AlertStatus status;

	@Column(nullable = false, columnDefinition = "text")
	private String message;

	@Column(name = "detected_at", nullable = false, updatable = false)
	private Instant detectedAt;

	@Column(name = "acknowledged_at")
	private Instant acknowledgedAt;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	protected Alert() {
	}

	public Alert(
			Drone drone,
			Mission mission,
			Geofence geofence,
			AlertType type,
			AlertSeverity severity,
			String message,
			Instant detectedAt) {
		this.drone = Objects.requireNonNull(drone, "drone must not be null");
		this.mission = mission;
		this.geofence = geofence;
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.severity = Objects.requireNonNull(severity, "severity must not be null");
		this.message = requireMessage(message);
		this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt must not be null");
		this.status = AlertStatus.OPEN;
	}

	public void acknowledge(Instant occurredAt) {
		Instant validOccurrence = requireValidOccurrence(occurredAt);
		transitionTo(AlertStatus.ACKNOWLEDGED);
		acknowledgedAt = validOccurrence;
	}

	public void resolve(Instant occurredAt) {
		Instant validOccurrence = requireValidOccurrence(occurredAt);
		transitionTo(AlertStatus.RESOLVED);
		resolvedAt = validOccurrence;
	}

	private void transitionTo(AlertStatus target) {
		if (!status.canTransitionTo(target)) {
			throw new InvalidAlertStatusTransitionException(status, target);
		}
		status = target;
	}

	private Instant requireValidOccurrence(Instant occurredAt) {
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		if (occurredAt.isBefore(detectedAt)) {
			throw new IllegalArgumentException("Alert status timestamp must not precede detection");
		}
		return occurredAt;
	}

	private static String requireMessage(String message) {
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("message must not be blank");
		}
		return message.trim();
	}

	public UUID getId() {
		return id;
	}

	public Drone getDrone() {
		return drone;
	}

	public Mission getMission() {
		return mission;
	}

	public Geofence getGeofence() {
		return geofence;
	}

	public AlertType getType() {
		return type;
	}

	public AlertSeverity getSeverity() {
		return severity;
	}

	public AlertStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public Instant getDetectedAt() {
		return detectedAt;
	}

	public Instant getAcknowledgedAt() {
		return acknowledgedAt;
	}

	public Instant getResolvedAt() {
		return resolvedAt;
	}
}
