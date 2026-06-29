package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

import com.portfolio.mini_utm.realtime.config.RealtimeWebSocketConfiguration;
import com.portfolio.mini_utm.realtime.config.RealtimeWebSocketProperties;

@TestPropertySource(properties =
		"mini-utm.realtime.websocket.allowed-origins=http://localhost:3000,http://localhost:5173")
class RealtimeWebSocketConfigurationTests extends PostgresIntegrationTest {

	@Autowired
	private RealtimeWebSocketProperties properties;

	@Autowired
	@Qualifier("stompWebSocketHandlerMapping")
	private AbstractUrlHandlerMapping webSocketHandlerMapping;

	@Autowired
	private SimpAnnotationMethodMessageHandler annotationMethodMessageHandler;

	@Autowired
	private SimpleBrokerMessageHandler simpleBrokerMessageHandler;

	@Test
	void exposeNativeStompEndpointWithConfiguredOrigins() {
		assertThat(webSocketHandlerMapping.getHandlerMap())
				.containsKey(RealtimeWebSocketConfiguration.ENDPOINT);
		assertThat(properties.allowedOrigins())
				.containsExactly("http://localhost:3000", "http://localhost:5173");
	}

	@Test
	void configureApplicationAndTopicDestinations() {
		assertThat(annotationMethodMessageHandler.getDestinationPrefixes())
				.containsExactly(RealtimeWebSocketConfiguration.APPLICATION_PREFIX + "/");
		assertThat(simpleBrokerMessageHandler.getDestinationPrefixes())
				.containsExactly(RealtimeWebSocketConfiguration.TOPIC_PREFIX);
	}
}
