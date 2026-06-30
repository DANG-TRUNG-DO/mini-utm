package com.portfolio.mini_utm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.portfolio.mini_utm.alert.application.AlertService;
import com.portfolio.mini_utm.alert.application.RaiseAlertCommand;
import com.portfolio.mini_utm.alert.domain.Alert;
import com.portfolio.mini_utm.alert.domain.AlertSeverity;
import com.portfolio.mini_utm.alert.domain.AlertStatus;
import com.portfolio.mini_utm.alert.domain.AlertType;
import com.portfolio.mini_utm.alert.repository.AlertRepository;
import com.portfolio.mini_utm.drone.domain.Drone;
import com.portfolio.mini_utm.drone.domain.DroneStatus;
import com.portfolio.mini_utm.drone.repository.DroneRepository;
import com.portfolio.mini_utm.realtime.application.RealtimePublisher;
import com.portfolio.mini_utm.realtime.message.AlertRealtimeMessage;
import com.portfolio.mini_utm.telemetry.repository.TelemetryRepository;

class AlertRealtimeIntegrationTests extends PostgresIntegrationTest {

	private static final Instant BASE_TIME = Instant.parse("2026-07-01T05:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AlertService alertService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private AlertRepository alertRepository;

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
	void publishCreateRefreshAndRecoveryAfterAlertCommits() throws Exception {
		Drone drone = activeDrone("ALERT-REALTIME-UAV-001");

		ingest(drone, "15.00", BASE_TIME);
		ingest(drone, "10.00", BASE_TIME.plusSeconds(10));
		ingest(drone, "30.00", BASE_TIME.plusSeconds(20));

		ArgumentCaptor<AlertRealtimeMessage> captor =
				ArgumentCaptor.forClass(AlertRealtimeMessage.class);
		verify(realtimePublisher, times(3)).publishAlert(captor.capture());
		var messages = captor.getAllValues();

		assertThat(messages.get(0).status()).isEqualTo(AlertStatus.OPEN);
		assertThat(messages.get(0).occurrenceCount()).isEqualTo(1);
		assertThat(messages.get(1).status()).isEqualTo(AlertStatus.OPEN);
		assertThat(messages.get(1).occurrenceCount()).isEqualTo(2);
		assertThat(messages.get(1).lastDetectedAt()).isEqualTo(BASE_TIME.plusSeconds(10));
		assertThat(messages.get(2).status()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(messages.get(2).resolvedAt()).isEqualTo(BASE_TIME.plusSeconds(20));
		assertThat(messages).extracting(AlertRealtimeMessage::alertId).doesNotContainNull();
		assertThat(messages).extracting(AlertRealtimeMessage::droneId)
				.containsOnly(drone.getId());
	}

	@Test
	void doNotPublishAlertWhenLifecycleTransactionRollsBack() {
		Drone drone = activeDrone("ALERT-REALTIME-UAV-002");
		var alert = raiseAlert(drone);
		reset(realtimePublisher);

		transactionTemplate.executeWithoutResult(status -> {
			alertService.acknowledge(alert.getId());
			status.setRollbackOnly();
		});

		verifyNoInteractions(realtimePublisher);
		assertThat(alertRepository.findById(alert.getId()).orElseThrow().getStatus())
				.isEqualTo(AlertStatus.OPEN);
	}

	@Test
	void keepCommittedAlertWhenRealtimePublisherFails() throws Exception {
		Drone drone = activeDrone("ALERT-REALTIME-UAV-003");
		var alert = raiseAlert(drone);
		reset(realtimePublisher);
		doThrow(new IllegalStateException("Broker unavailable"))
				.when(realtimePublisher).publishAlert(any());

		mockMvc.perform(post("/api/v1/alerts/{id}/acknowledge", alert.getId()))
				.andExpect(status().isOk());

		assertThat(alertRepository.findById(alert.getId()).orElseThrow().getStatus())
				.isEqualTo(AlertStatus.ACKNOWLEDGED);
	}

	private Alert raiseAlert(Drone drone) {
		return alertService.raiseOrRefresh(new RaiseAlertCommand(
				drone.getId(),
				null,
				null,
				AlertType.LOW_BATTERY,
				AlertSeverity.WARNING,
				"battery",
				"Battery is low",
				Instant.now().minusSeconds(5)))
				.alert();
	}

	private void ingest(Drone drone, String batteryPercent, Instant recordedAt) throws Exception {
		String request = """
				{
				  "droneId":"%s",
				  "longitude":106.700,
				  "latitude":10.780,
				  "altitudeM":75.50,
				  "speedMps":12.345,
				  "headingDegrees":181.50,
				  "batteryPercent":%s,
				  "recordedAt":"%s"
				}
				""".formatted(drone.getId(), batteryPercent, recordedAt);

		mockMvc.perform(post("/api/v1/telemetry")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated());
	}

	private Drone activeDrone(String serialNumber) {
		Drone drone = new Drone(serialNumber, "Realtime alert drone", "Quad-X");
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
}
