package com.portfolio.mini_utm.realtime.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(RealtimeWebSocketProperties.class)
public class RealtimeWebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	public static final String ENDPOINT = "/ws";
	public static final String TOPIC_PREFIX = "/topic";
	public static final String APPLICATION_PREFIX = "/app";

	private final RealtimeWebSocketProperties properties;

	public RealtimeWebSocketConfiguration(RealtimeWebSocketProperties properties) {
		this.properties = properties;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker(TOPIC_PREFIX);
		registry.setApplicationDestinationPrefixes(APPLICATION_PREFIX);
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(ENDPOINT)
				.setAllowedOrigins(properties.allowedOrigins().toArray(String[]::new));
	}
}
