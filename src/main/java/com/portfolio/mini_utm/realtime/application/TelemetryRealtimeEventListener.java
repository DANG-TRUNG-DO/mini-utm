package com.portfolio.mini_utm.realtime.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.portfolio.mini_utm.realtime.message.TelemetryRealtimeMessage;
import com.portfolio.mini_utm.telemetry.application.TelemetryIngestedEvent;

@Component
public class TelemetryRealtimeEventListener {

	private static final Logger log = LoggerFactory.getLogger(TelemetryRealtimeEventListener.class);

	private final RealtimePublisher realtimePublisher;

	public TelemetryRealtimeEventListener(RealtimePublisher realtimePublisher) {
		this.realtimePublisher = realtimePublisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onTelemetryIngested(TelemetryIngestedEvent event) {
		try {
			realtimePublisher.publishTelemetry(
					TelemetryRealtimeMessage.from(event.telemetry()));
		} catch (RuntimeException exception) {
			log.warn(
					"Could not publish telemetry {} for drone {}",
					event.telemetry().id(),
					event.telemetry().droneId(),
					exception);
		}
	}
}
