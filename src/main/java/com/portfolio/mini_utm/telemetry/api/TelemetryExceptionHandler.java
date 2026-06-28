package com.portfolio.mini_utm.telemetry.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.portfolio.mini_utm.telemetry.application.DuplicateTelemetryException;
import com.portfolio.mini_utm.telemetry.application.InvalidTelemetryException;

@RestControllerAdvice
public class TelemetryExceptionHandler {

	@ExceptionHandler(DuplicateTelemetryException.class)
	ProblemDetail handleConflict(DuplicateTelemetryException exception) {
		return problem(HttpStatus.CONFLICT, "Telemetry conflict", exception);
	}

	@ExceptionHandler(InvalidTelemetryException.class)
	ProblemDetail handleInvalid(InvalidTelemetryException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid telemetry", exception);
	}

	private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
		detail.setTitle(title);
		return detail;
	}
}
