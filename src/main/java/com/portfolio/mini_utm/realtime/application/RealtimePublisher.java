package com.portfolio.mini_utm.realtime.application;

import com.portfolio.mini_utm.realtime.message.AlertRealtimeMessage;
import com.portfolio.mini_utm.realtime.message.TelemetryRealtimeMessage;

public interface RealtimePublisher {

	void publishTelemetry(TelemetryRealtimeMessage message);

	void publishAlert(AlertRealtimeMessage message);
}
