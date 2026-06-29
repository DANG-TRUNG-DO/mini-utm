package com.portfolio.mini_utm.alert.application.rule;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "mini-utm.alert.route-deviation")
public record RouteDeviationAlertProperties(
		@NotNull @DecimalMin(value = "0.0", inclusive = false)
		BigDecimal corridorWidthMeters) {
}
