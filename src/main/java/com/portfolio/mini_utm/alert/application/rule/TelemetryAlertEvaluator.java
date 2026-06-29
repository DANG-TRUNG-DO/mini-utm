package com.portfolio.mini_utm.alert.application.rule;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

@Component
public class TelemetryAlertEvaluator {

	private static final Logger log = LoggerFactory.getLogger(TelemetryAlertEvaluator.class);

	private final List<TelemetryAlertRule> rules;

	public TelemetryAlertEvaluator(List<TelemetryAlertRule> rules) {
		this.rules = List.copyOf(rules);
	}

	public void evaluate(TelemetryResponse telemetry) {
		for (TelemetryAlertRule rule : rules) {
			try {
				rule.evaluate(telemetry);
			} catch (RuntimeException exception) {
				log.warn(
						"Alert rule {} failed for telemetry {} and drone {}",
						rule.getClass().getSimpleName(),
						telemetry.id(),
						telemetry.droneId(),
						exception);
			}
		}
	}
}
