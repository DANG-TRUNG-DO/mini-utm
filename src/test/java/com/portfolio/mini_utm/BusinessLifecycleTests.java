package com.portfolio.mini_utm;

import static com.portfolio.mini_utm.alert.domain.AlertStatus.ACKNOWLEDGED;
import static com.portfolio.mini_utm.alert.domain.AlertStatus.OPEN;
import static com.portfolio.mini_utm.alert.domain.AlertStatus.RESOLVED;
import static com.portfolio.mini_utm.drone.domain.DroneStatus.ACTIVE;
import static com.portfolio.mini_utm.drone.domain.DroneStatus.INACTIVE;
import static com.portfolio.mini_utm.drone.domain.DroneStatus.MAINTENANCE;
import static com.portfolio.mini_utm.mission.domain.MissionStatus.APPROVED;
import static com.portfolio.mini_utm.mission.domain.MissionStatus.CANCELLED;
import static com.portfolio.mini_utm.mission.domain.MissionStatus.COMPLETED;
import static com.portfolio.mini_utm.mission.domain.MissionStatus.PLANNED;
import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.mini_utm.mission.domain.MissionStatus;
import org.junit.jupiter.api.Test;

class BusinessLifecycleTests {

	@Test
	void droneLifecycleRequiresMaintenanceToReturnThroughInactive() {
		assertThat(INACTIVE.canTransitionTo(ACTIVE)).isTrue();
		assertThat(ACTIVE.canTransitionTo(MAINTENANCE)).isTrue();
		assertThat(MAINTENANCE.canTransitionTo(INACTIVE)).isTrue();
		assertThat(MAINTENANCE.canTransitionTo(ACTIVE)).isFalse();
	}

	@Test
	void missionLifecycleRequiresApprovalBeforeActivationAndHasTerminalStates() {
		assertThat(PLANNED.canTransitionTo(APPROVED)).isTrue();
		assertThat(PLANNED.canTransitionTo(MissionStatus.ACTIVE)).isFalse();
		assertThat(APPROVED.canTransitionTo(MissionStatus.ACTIVE)).isTrue();
		assertThat(COMPLETED.canTransitionTo(CANCELLED)).isFalse();
		assertThat(CANCELLED.canTransitionTo(PLANNED)).isFalse();
	}

	@Test
	void alertLifecycleCanBeResolvedDirectlyOrAfterAcknowledgement() {
		assertThat(OPEN.canTransitionTo(ACKNOWLEDGED)).isTrue();
		assertThat(OPEN.canTransitionTo(RESOLVED)).isTrue();
		assertThat(ACKNOWLEDGED.canTransitionTo(RESOLVED)).isTrue();
		assertThat(RESOLVED.canTransitionTo(OPEN)).isFalse();
	}
}
