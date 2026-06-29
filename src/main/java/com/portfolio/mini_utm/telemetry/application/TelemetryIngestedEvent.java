package com.portfolio.mini_utm.telemetry.application;

import com.portfolio.mini_utm.telemetry.api.dto.TelemetryResponse;

public record TelemetryIngestedEvent(TelemetryResponse telemetry) {
}
