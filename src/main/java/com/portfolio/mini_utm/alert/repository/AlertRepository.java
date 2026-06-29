package com.portfolio.mini_utm.alert.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;

import jakarta.persistence.LockModeType;

public interface AlertRepository
		extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {

	@Override
	@EntityGraph(attributePaths = {"drone", "mission", "geofence"})
	Page<Alert> findAll(Specification<Alert> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"drone", "mission", "geofence"})
	@Query("SELECT a FROM Alert a WHERE a.id = :id")
	Optional<Alert> findDetailsById(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Alert a WHERE a.id = :id")
	Optional<Alert> findByIdForUpdate(@Param("id") UUID id);

	Optional<Alert> findByDroneIdAndTypeAndDedupKeyAndStatusNot(
			UUID droneId,
			AlertType type,
			String dedupKey,
			AlertStatus excludedStatus);

	List<Alert> findByDroneIdAndTypeAndStatusNot(
			UUID droneId,
			AlertType type,
			AlertStatus excludedStatus);
}
