package com.portfolio.mini_utm.mission.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.mini_utm.mission.domain.MissionWaypoint;

public interface MissionWaypointRepository extends JpaRepository<MissionWaypoint, Long> {
}
