package com.portfolio.mini_utm.alert.application;

import com.portfolio.mini_utm.alert.domain.Alert;

public record RaiseAlertResult(Alert alert, boolean created) {
}
