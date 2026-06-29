package com.portfolio.mini_utm.telemetry.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "mini-utm.telemetry.retention")
public record TelemetryRetentionProperties(
		@Min(1) int days,
		@Min(1) int batchSize) {
}
