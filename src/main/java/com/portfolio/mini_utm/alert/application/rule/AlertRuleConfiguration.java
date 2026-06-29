package com.portfolio.mini_utm.alert.application.rule;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LowBatteryAlertProperties.class)
public class AlertRuleConfiguration {
}
