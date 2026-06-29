package com.portfolio.mini_utm.alert.application.rule;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "mini-utm.alert.low-battery")
public record LowBatteryAlertProperties(
		@NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal thresholdPercent,
		@NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal recoveryThresholdPercent) {

	public LowBatteryAlertProperties {
		if (thresholdPercent != null
				&& recoveryThresholdPercent != null
				&& recoveryThresholdPercent.compareTo(thresholdPercent) <= 0) {
			throw new IllegalArgumentException(
					"Low battery recovery threshold must be greater than alert threshold");
		}
	}
}
