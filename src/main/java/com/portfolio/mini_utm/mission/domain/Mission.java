package com.portfolio.mini_utm.mission.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.LineString;

import com.portfolio.mini_utm.drone.domain.Drone;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "missions")
public class Mission {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "drone_id", nullable = false)
	private Drone drone;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MissionStatus status;

	@Column(name = "planned_path", columnDefinition = "geometry(LineStringZ,4326)")
	private LineString plannedPath;

	@Column(name = "planned_start_at")
	private Instant plannedStartAt;

	@Column(name = "planned_end_at")
	private Instant plannedEndAt;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequenceNumber ASC")
	private List<MissionWaypoint> waypoints = new ArrayList<>();

	protected Mission() {
	}

	public Mission(
			Drone drone,
			String name,
			LineString plannedPath,
			Instant plannedStartAt,
			Instant plannedEndAt) {
		this.drone = drone;
		this.name = name;
		this.plannedPath = plannedPath;
		this.plannedStartAt = plannedStartAt;
		this.plannedEndAt = plannedEndAt;
		this.status = MissionStatus.PLANNED;
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

	public void addWaypoint(MissionWaypoint waypoint) {
		waypoint.assignTo(this);
		waypoints.add(waypoint);
	}

	public void approve(Instant occurredAt) {
		transitionTo(MissionStatus.APPROVED);
		approvedAt = occurredAt;
	}

	public void start(Instant occurredAt) {
		transitionTo(MissionStatus.ACTIVE);
		startedAt = occurredAt;
	}

	public void complete(Instant occurredAt) {
		transitionTo(MissionStatus.COMPLETED);
		completedAt = occurredAt;
	}

	public void cancel() {
		transitionTo(MissionStatus.CANCELLED);
	}

	private void transitionTo(MissionStatus target) {
		if (!status.canTransitionTo(target)) {
			throw new InvalidMissionStatusTransitionException(status, target);
		}
		status = target;
	}

	public UUID getId() {
		return id;
	}

	public Drone getDrone() {
		return drone;
	}

	public String getName() {
		return name;
	}

	public MissionStatus getStatus() {
		return status;
	}

	public LineString getPlannedPath() {
		return plannedPath;
	}

	public Instant getPlannedStartAt() {
		return plannedStartAt;
	}

	public Instant getPlannedEndAt() {
		return plannedEndAt;
	}

	public Instant getApprovedAt() {
		return approvedAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<MissionWaypoint> getWaypoints() {
		return Collections.unmodifiableList(waypoints);
	}
}
