package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.realtime.application.RealtimePublisher;
import com.portfolio.mini_utm.realtime.message.TelemetryRealtimeMessage;
import com.portfolio.mini_utm.telemetry.api.dto.IngestTelemetryRequest;
import com.portfolio.mini_utm.telemetry.application.TelemetryService;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class TelemetryRealtimeIntegrationTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TelemetryService telemetryService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private TelemetryRepository telemetryRepository;

	@Autowired
	private DroneRepository droneRepository;

	@MockitoBean
	private RealtimePublisher realtimePublisher;

	@BeforeEach
	void cleanBefore() {
		cleanDatabase();
	}

	@AfterEach
	void cleanAfter() {
		cleanDatabase();
	}

	@Test
	void publishTelemetryAfterSuccessfulCommit() throws Exception {
		Drone drone = activeDrone("REALTIME-UAV-001");

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest(drone.getId(), "2026-07-01T01:15:00Z")))
				.andExpect(status().isCreated());

		ArgumentCaptor<TelemetryRealtimeMessage> messageCaptor =
				ArgumentCaptor.forClass(TelemetryRealtimeMessage.class);
		verify(realtimePublisher).publishTelemetry(messageCaptor.capture());
		TelemetryRealtimeMessage message = messageCaptor.getValue();
		assertThat(message.telemetryId()).isNotNull();
		assertThat(message.droneId()).isEqualTo(drone.getId());
		assertThat(message.missionId()).isNull();
		assertThat(message.longitude()).isEqualTo(106.700);
		assertThat(message.latitude()).isEqualTo(10.780);
		assertThat(message.recordedAt()).isEqualTo(Instant.parse("2026-07-01T01:15:00Z"));
	}

	@Test
	void doNotPublishTelemetryWhenTransactionRollsBack() {
		Drone drone = activeDrone("REALTIME-UAV-002");
		IngestTelemetryRequest request = request(
				drone.getId(), Instant.parse("2026-07-01T01:20:00Z"));

		transactionTemplate.executeWithoutResult(status -> {
			telemetryService.ingest(request);
			status.setRollbackOnly();
		});

		verifyNoInteractions(realtimePublisher);
		assertThat(telemetryRepository.count()).isZero();
	}

	@Test
	void keepSuccessfulRestResponseWhenRealtimePublisherFails() throws Exception {
		Drone drone = activeDrone("REALTIME-UAV-003");
		doThrow(new IllegalStateException("Broker unavailable"))
				.when(realtimePublisher).publishTelemetry(any());

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequest(drone.getId(), "2026-07-01T01:25:00Z")))
				.andExpect(status().isCreated());

		assertThat(telemetryRepository.count()).isEqualTo(1);
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Realtime telemetry drone", "Quad-X");
		drone.transitionTo(DroneStatus.ACTIVE);
		return droneRepository.saveAndFlush(drone);
	}

	private IngestTelemetryRequest request(UUID droneId, Instant recordedAt) {
		return new IngestTelemetryRequest(
				droneId,
				null,
				106.700,
				10.780,
				new BigDecimal("75.50"),
				new BigDecimal("12.345"),
				new BigDecimal("181.50"),
				new BigDecimal("82.25"),
				recordedAt);
	}

	private String validRequest(UUID droneId, String recordedAt) {
		return """
				{
				  "droneId":"%s",
				  "longitude":106.700,
				  "latitude":10.780,
				  "altitudeM":75.50,
				  "speedMps":12.345,
				  "headingDegrees":181.50,
				  "batteryPercent":82.25,
				  "recordedAt":"%s"
				}
				""".formatted(droneId, recordedAt);
	}

	private void cleanDatabase() {
		telemetryRepository.deleteAll();
		telemetryRepository.flush();
		droneRepository.deleteAll();
		droneRepository.flush();
	}
}
