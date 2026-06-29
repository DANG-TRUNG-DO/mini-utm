package com.portfolio.mini_utm.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.domain.InvalidAlertStatusTransitionException;
import com.portfolio.mini_utm.drone.domain.Drone;

class AlertDomainTests {

	private static final Instant DETECTED_AT = Instant.parse("2026-07-01T03:00:00Z");

	@Test
	void acknowledgeThenResolveAlert() {
		Alert alert = alert();
		Instant acknowledgedAt = DETECTED_AT.plusSeconds(10);
		Instant resolvedAt = DETECTED_AT.plusSeconds(20);

		alert.acknowledge(acknowledgedAt);
		alert.resolve(resolvedAt);

		assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(alert.getAcknowledgedAt()).isEqualTo(acknowledgedAt);
		assertThat(alert.getResolvedAt()).isEqualTo(resolvedAt);
	}

	@Test
	void resolveOpenAlertDirectly() {
		Alert alert = alert();

		alert.resolve(DETECTED_AT.plusSeconds(10));

		assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(alert.getAcknowledgedAt()).isNull();
	}

	@Test
	void rejectInvalidTransitionAndTimestampBeforeDetection() {
		Alert resolved = alert();
		resolved.resolve(DETECTED_AT.plusSeconds(10));

		assertThatThrownBy(() -> resolved.acknowledge(DETECTED_AT.plusSeconds(20)))
				.isInstanceOf(InvalidAlertStatusTransitionException.class);
		Alert invalidTimestamp = alert();
		assertThatThrownBy(() -> invalidTimestamp.acknowledge(DETECTED_AT.minusSeconds(1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not precede detection");
		assertThat(invalidTimestamp.getStatus()).isEqualTo(AlertStatus.OPEN);
	}

	private Alert alert() {
		return new Alert(
				new Drone("ALERT-DOMAIN-UAV", "Alert domain drone", "Quad-X"),
				null,
				null,
				AlertType.LOW_BATTERY,
				AlertSeverity.WARNING,
				"Battery level is below threshold",
				DETECTED_AT);
	}
}
