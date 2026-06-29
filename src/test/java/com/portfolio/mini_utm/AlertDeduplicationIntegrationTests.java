package com.portfolio.mini_utm;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.application.RaiseAlertCommand;
import com.portfolio.mini_utm.alert.application.RaiseAlertResult;
import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.repository.DroneRepository;

class AlertDeduplicationIntegrationTests extends PostgresIntegrationTest {

	private static final Instant DETECTED_AT = Instant.parse("2026-07-01T03:00:00Z");

	@Autowired
	private AlertService alertService;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private DroneRepository droneRepository;

	@BeforeEach
	void cleanBefore() {
		cleanDatabase();
	}

	@AfterEach
	void cleanAfter() {
		cleanDatabase();
	}

	@Test
	void refreshExistingActiveAlertForSameDeduplicationKey() {
		Drone drone = drone("ALERT-DEDUP-UAV-001");

		RaiseAlertResult first = alertService.raiseOrRefresh(command(
				drone, " Battery-Threshold ", AlertSeverity.WARNING,
				"Battery is below 20 percent", DETECTED_AT));
		RaiseAlertResult repeated = alertService.raiseOrRefresh(command(
				drone, "battery-threshold", AlertSeverity.CRITICAL,
				"Battery is below 10 percent", DETECTED_AT.plusSeconds(10)));

		Alert stored = alertRepository.findAll().get(0);
		assertThat(first.created()).isTrue();
		assertThat(repeated.created()).isFalse();
		assertThat(repeated.alert().getId()).isEqualTo(first.alert().getId());
		assertThat(alertRepository.count()).isEqualTo(1);
		assertThat(stored.getDedupKey()).isEqualTo("battery-threshold");
		assertThat(stored.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(stored.getMessage()).isEqualTo("Battery is below 10 percent");
		assertThat(stored.getLastDetectedAt()).isEqualTo(DETECTED_AT.plusSeconds(10));
		assertThat(stored.getOccurrenceCount()).isEqualTo(2);
	}

	@Test
	void createNewAlertWhenPreviousOccurrenceHasBeenResolved() {
		Drone drone = drone("ALERT-DEDUP-UAV-002");
		RaiseAlertResult first = alertService.raiseOrRefresh(command(
				drone, "battery-threshold", AlertSeverity.WARNING,
				"Battery is below threshold", DETECTED_AT));
		Alert resolved = alertRepository.findById(first.alert().getId()).orElseThrow();
		resolved.resolve(DETECTED_AT.plusSeconds(20));
		alertRepository.saveAndFlush(resolved);

		RaiseAlertResult recurrence = alertService.raiseOrRefresh(command(
				drone, "battery-threshold", AlertSeverity.WARNING,
				"Battery is below threshold again", DETECTED_AT.plusSeconds(30)));

		assertThat(recurrence.created()).isTrue();
		assertThat(recurrence.alert().getId()).isNotEqualTo(first.alert().getId());
		assertThat(alertRepository.count()).isEqualTo(2);
	}

	@Test
	void serializeConcurrentDetectionsForTheSameDrone() throws Exception {
		Drone drone = drone("ALERT-DEDUP-UAV-003");
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			CompletableFuture<RaiseAlertResult> first = concurrentRaise(
					executor, ready, start, drone);
			CompletableFuture<RaiseAlertResult> second = concurrentRaise(
					executor, ready, start, drone);
			assertThat(ready.await(5, SECONDS)).isTrue();
			start.countDown();

			List<RaiseAlertResult> results = List.of(
					first.get(10, SECONDS), second.get(10, SECONDS));

			assertThat(results).extracting(RaiseAlertResult::created)
					.containsExactlyInAnyOrder(true, false);
			assertThat(results).extracting(result -> result.alert().getId())
					.containsOnly(results.get(0).alert().getId());
			assertThat(alertRepository.count()).isEqualTo(1);
			assertThat(alertRepository.findAll().get(0).getOccurrenceCount()).isEqualTo(2);
		} finally {
			executor.shutdownNow();
		}
	}

	private CompletableFuture<RaiseAlertResult> concurrentRaise(
			ExecutorService executor,
			CountDownLatch ready,
			CountDownLatch start,
			Drone drone) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				ready.countDown();
				start.await(5, SECONDS);
				return alertService.raiseOrRefresh(command(
						drone, "battery-threshold", AlertSeverity.WARNING,
						"Concurrent low battery detection", DETECTED_AT));
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(exception);
			}
		}, executor);
	}

	private RaiseAlertCommand command(
			Drone drone,
			String dedupKey,
			AlertSeverity severity,
			String message,
			Instant detectedAt) {
		return new RaiseAlertCommand(
				drone.getId(),
				null,
				null,
				AlertType.LOW_BATTERY,
				severity,
				dedupKey,
				message,
				detectedAt);
	}

	private Drone drone(String serialNumber) {
		return droneRepository.saveAndFlush(
				new Drone(serialNumber, "Alert deduplication drone", "Quad-X"));
	}

	private void cleanDatabase() {
		alertRepository.deleteAll();
		alertRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
