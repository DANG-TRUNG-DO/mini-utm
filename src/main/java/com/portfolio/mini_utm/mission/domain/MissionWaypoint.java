package com.portfolio.mini_utm.mission.domain;

import java.time.Instant;

import org.locationtech.jts.geom.Point;

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
@Table(name = "mission_waypoints")
public class MissionWaypoint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mission_id", nullable = false)
	private Mission mission;

	@Column(name = "sequence_number", nullable = false)
	private int sequenceNumber;

	@Column(nullable = false, columnDefinition = "geometry(PointZ,4326)")
	private Point position;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected MissionWaypoint() {
	}

	public MissionWaypoint(int sequenceNumber, Point position) {
		this.sequenceNumber = sequenceNumber;
		this.position = position;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	void assignTo(Mission mission) {
		this.mission = mission;
	}

	public Long getId() {
		return id;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public Point getPosition() {
		return position;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
