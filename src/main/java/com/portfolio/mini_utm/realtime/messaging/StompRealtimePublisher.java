package com.portfolio.mini_utm.realtime.messaging;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.realtime.application.RealtimePublisher;
import com.portfolio.mini_utm.realtime.message.AlertRealtimeMessage;
import com.portfolio.mini_utm.realtime.message.TelemetryRealtimeMessage;

@Component
public class StompRealtimePublisher implements RealtimePublisher {

	private static final String DRONE_TOPIC_PREFIX = "/topic/drones/";

	private final SimpMessagingTemplate messagingTemplate;

	public StompRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void publishTelemetry(TelemetryRealtimeMessage message) {
		messagingTemplate.convertAndSend(telemetryDestination(message.droneId()), message);
	}

	@Override
	public void publishAlert(AlertRealtimeMessage message) {
		messagingTemplate.convertAndSend(alertDestination(message.droneId()), message);
	}

	public static String telemetryDestination(UUID droneId) {
		return DRONE_TOPIC_PREFIX + droneId + "/telemetry";
	}

	public static String alertDestination(UUID droneId) {
		return DRONE_TOPIC_PREFIX + droneId + "/alerts";
	}
}
