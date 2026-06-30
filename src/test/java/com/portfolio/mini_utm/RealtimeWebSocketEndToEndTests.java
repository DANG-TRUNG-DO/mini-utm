package com.portfolio.mini_utm;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.realtime.message.AlertRealtimeMessage;
import com.portfolio.mini_utm.realtime.message.TelemetryRealtimeMessage;
import com.portfolio.mini_utm.realtime.messaging.StompRealtimePublisher;
import com.portfolio.mini_utm.telemetry.api.dto.IngestTelemetryRequest;
import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "mini-utm.realtime.websocket.allowed-origins=*")
@Import(RealtimeWebSocketEndToEndTests.SubscriptionTestConfiguration.class)
class RealtimeWebSocketEndToEndTests extends PostgresIntegrationTest {

	@LocalServerPort
	private int serverPort;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TelemetryRepository telemetryRepository;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private DroneRepository droneRepository;

	@Autowired
	private SubscriptionProbe subscriptionProbe;

	private WebSocketStompClient stompClient;
	private StompSession stompSession;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(objectMapper);
		stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		stompClient.setMessageConverter(converter);
	}

	@AfterEach
	void tearDown() {
		if (stompSession != null && stompSession.isConnected()) {
			stompSession.disconnect();
		}
		if (stompClient != null) {
			stompClient.stop();
		}
		cleanDatabase();
	}

	@Test
	void deliverIngestedTelemetryToSubscribedDroneTopic() throws Exception {
		Drone drone = activeDrone("WS-E2E-UAV-001");
		connect();
		MessageCollector collector = subscribe(drone);

		ResponseEntity<TelemetryResponse> response = ingest(
				drone, Instant.parse("2026-07-01T03:00:00Z"));
		TelemetryRealtimeMessage message = collector.nextMessage().get(5, SECONDS);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(message.telemetryId()).isEqualTo(response.getBody().id());
		assertThat(message.droneId()).isEqualTo(drone.getId());
		assertThat(message.longitude()).isEqualTo(106.700);
		assertThat(message.latitude()).isEqualTo(10.780);
		assertThat(message.recordedAt()).isEqualTo(Instant.parse("2026-07-01T03:00:00Z"));
	}

	@Test
	void isolateTelemetrySubscriptionsByDrone() throws Exception {
		Drone droneA = activeDrone("WS-E2E-UAV-002");
		Drone droneB = activeDrone("WS-E2E-UAV-003");
		connect();
		MessageCollector collectorA = subscribe(droneA);
		MessageCollector collectorB = subscribe(droneB);

		ingest(droneB, Instant.parse("2026-07-01T03:05:00Z"));

		assertThat(collectorB.nextMessage().get(5, SECONDS).droneId())
				.isEqualTo(droneB.getId());
		assertThrows(TimeoutException.class,
				() -> collectorA.nextMessage().get(500, MILLISECONDS));

		ingest(droneA, Instant.parse("2026-07-01T03:06:00Z"));
		assertThat(collectorA.nextMessage().get(5, SECONDS).droneId())
				.isEqualTo(droneA.getId());
	}

	@Test
	void deliverAlertLifecycleToSubscribedDroneTopic() throws Exception {
		Drone drone = activeDrone("WS-ALERT-E2E-UAV-001");
		connect();
		AlertMessageCollector collector = subscribeAlerts(drone);

		ResponseEntity<TelemetryResponse> lowBatteryResponse = ingest(
				drone,
				new BigDecimal("15.00"),
				Instant.parse("2026-07-01T04:00:00Z"));
		AlertRealtimeMessage opened = collector.nextMessage(5, SECONDS);

		assertThat(lowBatteryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(opened.alertId()).isNotNull();
		assertThat(opened.droneId()).isEqualTo(drone.getId());
		assertThat(opened.type()).isEqualTo(AlertType.LOW_BATTERY);
		assertThat(opened.status()).isEqualTo(AlertStatus.OPEN);
		assertThat(opened.occurrenceCount()).isEqualTo(1);

		ResponseEntity<TelemetryResponse> recoveredResponse = ingest(
				drone,
				new BigDecimal("30.00"),
				Instant.parse("2026-07-01T04:00:10Z"));
		AlertRealtimeMessage resolved = collector.nextMessage(5, SECONDS);

		assertThat(recoveredResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(resolved.alertId()).isEqualTo(opened.alertId());
		assertThat(resolved.status()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(resolved.resolvedAt()).isEqualTo(Instant.parse("2026-07-01T04:00:10Z"));
	}

	@Test
	void isolateAlertSubscriptionsByDrone() throws Exception {
		Drone droneA = activeDrone("WS-ALERT-E2E-UAV-002");
		Drone droneB = activeDrone("WS-ALERT-E2E-UAV-003");
		connect();
		AlertMessageCollector collectorA = subscribeAlerts(droneA);
		AlertMessageCollector collectorB = subscribeAlerts(droneB);

		ingest(
				droneB,
				new BigDecimal("12.00"),
				Instant.parse("2026-07-01T04:05:00Z"));

		assertThat(collectorB.nextMessage(5, SECONDS).droneId()).isEqualTo(droneB.getId());
		assertThat(collectorA.nextMessage(500, MILLISECONDS)).isNull();
	}

	private void connect() throws Exception {
		stompSession = stompClient.connectAsync(
				"ws://localhost:" + serverPort + "/ws",
				new StompSessionHandlerAdapter() {
				}).get(5, SECONDS);
	}

	private MessageCollector subscribe(Drone drone) throws Exception {
		MessageCollector collector = new MessageCollector();
		String destination = StompRealtimePublisher.telemetryDestination(drone.getId());
		stompSession.subscribe(destination, collector);
		subscriptionProbe.await(destination);
		return collector;
	}

	private AlertMessageCollector subscribeAlerts(Drone drone) throws Exception {
		AlertMessageCollector collector = new AlertMessageCollector();
		String destination = StompRealtimePublisher.alertDestination(drone.getId());
		stompSession.subscribe(destination, collector);
		subscriptionProbe.await(destination);
		return collector;
	}

	private ResponseEntity<TelemetryResponse> ingest(Drone drone, Instant recordedAt) {
		return ingest(drone, new BigDecimal("82.25"), recordedAt);
	}

	private ResponseEntity<TelemetryResponse> ingest(
			Drone drone,
			BigDecimal batteryPercent,
			Instant recordedAt) {
		return restTemplate.postForEntity(
				"/api/v1/telemetry",
				new IngestTelemetryRequest(
						drone.getId(),
						null,
						106.700,
						10.780,
						new BigDecimal("75.50"),
						new BigDecimal("12.345"),
						new BigDecimal("181.50"),
						batteryPercent,
						recordedAt),
				TelemetryResponse.class);
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "WebSocket E2E drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private void cleanDatabase() {
		alertRepository.deleteAll();
		alertRepository.flush();
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}

	private static final class AlertMessageCollector implements StompFrameHandler {

		private final BlockingQueue<AlertRealtimeMessage> messages = new LinkedBlockingQueue<>();

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return AlertRealtimeMessage.class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			messages.add((AlertRealtimeMessage) payload);
		}

		AlertRealtimeMessage nextMessage(long timeout, TimeUnit unit)
				throws InterruptedException {
			return messages.poll(timeout, unit);
		}
	}

	private static final class MessageCollector implements StompFrameHandler {

		private final CompletableFuture<TelemetryRealtimeMessage> nextMessage =
				new CompletableFuture<>();

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return TelemetryRealtimeMessage.class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			nextMessage.complete((TelemetryRealtimeMessage) payload);
		}

		CompletableFuture<TelemetryRealtimeMessage> nextMessage() {
			return nextMessage;
		}
	}

	private static final class SubscriptionProbe
			implements ApplicationListener<SessionSubscribeEvent> {

		private final BlockingQueue<String> destinations = new LinkedBlockingQueue<>();

		@Override
		public void onApplicationEvent(SessionSubscribeEvent event) {
			destinations.add(SimpMessageHeaderAccessor.getDestination(
					event.getMessage().getHeaders()));
		}

		void await(String expectedDestination) throws Exception {
			assertThat(destinations.poll(5, SECONDS)).isEqualTo(expectedDestination);
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class SubscriptionTestConfiguration {

		@Bean
		SubscriptionProbe subscriptionProbe() {
			return new SubscriptionProbe();
		}
	}
}
