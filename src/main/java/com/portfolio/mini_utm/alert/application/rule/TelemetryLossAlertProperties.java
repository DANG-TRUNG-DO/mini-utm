package com.portfolio.mini_utm.alert.application.rule;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

import static java.time.temporal.ChronoUnit.SECONDS;

@Validated
@ConfigurationProperties(prefix = "mini-utm.alert.telemetry-loss")
public record TelemetryLossAlertProperties(
		@NotNull @DurationUnit(SECONDS) Duration timeout) {

	public TelemetryLossAlertProperties {
		if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
			throw new IllegalArgumentException("Telemetry loss timeout must be positive");
		}
	}
}
