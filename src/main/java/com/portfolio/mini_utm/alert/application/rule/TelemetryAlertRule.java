package com.portfolio.mini_utm.alert.application.rule;

import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

public interface TelemetryAlertRule {

	void evaluate(TelemetryResponse telemetry);
}
