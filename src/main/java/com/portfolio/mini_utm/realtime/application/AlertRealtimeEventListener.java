package com.portfolio.mini_utm.realtime.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.portfolio.mini_utm.alert.application.AlertChangedEvent;
import com.portfolio.mini_utm.realtime.message.AlertRealtimeMessage;

@Component
public class AlertRealtimeEventListener {

	private static final Logger log = LoggerFactory.getLogger(AlertRealtimeEventListener.class);

	private final RealtimePublisher realtimePublisher;

	public AlertRealtimeEventListener(RealtimePublisher realtimePublisher) {
		this.realtimePublisher = realtimePublisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAlertChanged(AlertChangedEvent event) {
		try {
			realtimePublisher.publishAlert(AlertRealtimeMessage.from(event.alert()));
		} catch (RuntimeException exception) {
			log.warn(
					"Could not publish alert {} for drone {}",
					event.alert().id(),
					event.alert().droneId(),
					exception);
		}
	}
}
