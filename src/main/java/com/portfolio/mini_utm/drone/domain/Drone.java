package com.portfolio.mini_utm.drone.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "drones")
public class Drone {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "serial_number", nullable = false, unique = true, length = 100)
	private String serialNumber;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private DroneStatus status;

	@Column(length = 100)
	private String model;

	@Column(name = "registered_at", nullable = false, updatable = false)
	private Instant registeredAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Drone() {
	}

	public Drone(String serialNumber, String name, String model) {
		this.serialNumber = serialNumber;
		this.name = name;
		this.model = model;
		this.status = DroneStatus.INACTIVE;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (status == null) {
			status = DroneStatus.INACTIVE;
		}
		registeredAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void updateDetails(String name, String model) {
		if (name != null) {
			this.name = name;
		}
		if (model != null) {
			this.model = model;
		}
	}

	public void transitionTo(DroneStatus target) {
		if (!status.canTransitionTo(target)) {
			throw new InvalidDroneStatusTransitionException(status, target);
		}
		status = target;
	}

	public UUID getId() {
		return id;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public String getName() {
		return name;
	}

	public DroneStatus getStatus() {
		return status;
	}

	public String getModel() {
		return model;
	}

	public Instant getRegisteredAt() {
		return registeredAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
