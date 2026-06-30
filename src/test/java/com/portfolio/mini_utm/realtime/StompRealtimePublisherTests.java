package com.portfolio.mini_utm.realtime;

import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.realtime.message.AlertRealtimeMessage;
import com.portfolio.mini_utm.realtime.message.TelemetryRealtimeMessage;
import com.portfolio.mini_utm.realtime.messaging.StompRealtimePublisher;

@ExtendWith(MockitoExtension.class)
class StompRealtimePublisherTests {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private StompRealtimePublisher publisher;

	@Test
	void publishTelemetryToDroneSpecificTopic() {
		UUID droneId = UUID.randomUUID();
		TelemetryRealtimeMessage message = new TelemetryRealtimeMessage(
				42L,
				droneId,
				UUID.randomUUID(),
				106.700,
				10.780,
				new BigDecimal("75.00"),
				new BigDecimal("12.345"),
				new BigDecimal("181.50"),
				new BigDecimal("82.25"),
				Instant.parse("2026-06-29T03:00:00Z"));

		publisher.publishTelemetry(message);

		verify(messagingTemplate).convertAndSend(
				"/topic/drones/" + droneId + "/telemetry",
				message);
	}

	@Test
	void publishAlertToDroneSpecificTopic() {
		UUID droneId = UUID.randomUUID();
		AlertRealtimeMessage message = new AlertRealtimeMessage(
				UUID.randomUUID(),
				droneId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AlertType.GEOFENCE_VIOLATION,
				AlertSeverity.CRITICAL,
				AlertStatus.OPEN,
				"Drone entered a restricted geofence",
				Instant.parse("2026-06-29T03:00:00Z"),
				Instant.parse("2026-06-29T03:00:00Z"),
				1,
				null,
				null);

		publisher.publishAlert(message);

		verify(messagingTemplate).convertAndSend(
				"/topic/drones/" + droneId + "/alerts",
				message);
	}
}
