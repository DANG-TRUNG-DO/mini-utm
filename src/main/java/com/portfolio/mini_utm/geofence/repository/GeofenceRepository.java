package com.portfolio.mini_utm.geofence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.mini_utm.geofence.domain.Geofence;

public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {

	boolean existsByNameIgnoreCase(String name);
}
