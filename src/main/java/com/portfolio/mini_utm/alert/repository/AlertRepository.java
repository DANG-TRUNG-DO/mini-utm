package com.portfolio.mini_utm.alert.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

	Optional<Alert> findByDroneIdAndTypeAndDedupKeyAndStatusNot(
			UUID droneId,
			AlertType type,
			String dedupKey,
			AlertStatus excludedStatus);
}
