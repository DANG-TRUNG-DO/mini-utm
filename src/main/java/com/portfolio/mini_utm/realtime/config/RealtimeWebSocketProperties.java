package com.portfolio.mini_utm.realtime.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(prefix = "mini-utm.realtime.websocket")
public record RealtimeWebSocketProperties(
		@NotEmpty List<@NotBlank String> allowedOrigins) {
}
