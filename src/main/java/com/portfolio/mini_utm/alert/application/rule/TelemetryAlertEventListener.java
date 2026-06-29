package com.portfolio.mini_utm.alert.application.rule;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.portfolio.mini_utm.telemetry.application.TelemetryIngestedEvent;

@Component
public class TelemetryAlertEventListener {

	private final TelemetryAlertEvaluator evaluator;

	public TelemetryAlertEventListener(TelemetryAlertEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onTelemetryIngested(TelemetryIngestedEvent event) {
		evaluator.evaluate(event.telemetry());
	}
}
