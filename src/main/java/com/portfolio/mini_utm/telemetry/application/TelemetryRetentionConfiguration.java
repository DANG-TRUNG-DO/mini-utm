package com.portfolio.mini_utm.telemetry.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(TelemetryRetentionProperties.class)
public class TelemetryRetentionConfiguration {
}
