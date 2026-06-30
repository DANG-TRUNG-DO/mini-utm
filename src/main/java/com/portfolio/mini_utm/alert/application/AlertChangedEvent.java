package com.portfolio.mini_utm.alert.application;

import com.portfolio.mini_utm.alert.api.dto.AlertResponse;

public record AlertChangedEvent(AlertResponse alert) {
}
