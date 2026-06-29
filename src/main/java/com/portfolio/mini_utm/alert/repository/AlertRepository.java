package com.portfolio.mini_utm.alert.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.mini_utm.alert.domain.Alert;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
}
