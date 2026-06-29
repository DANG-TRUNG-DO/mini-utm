package com.portfolio.mini_utm.alert.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.portfolio.mini_utm.alert.application.AlertNotFoundException;
import com.portfolio.mini_utm.alert.application.InvalidAlertException;
import com.portfolio.mini_utm.alert.domain.InvalidAlertStatusTransitionException;

@RestControllerAdvice
public class AlertExceptionHandler {

	@ExceptionHandler(AlertNotFoundException.class)
	ProblemDetail handleNotFound(AlertNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Alert not found", exception);
	}

	@ExceptionHandler(InvalidAlertStatusTransitionException.class)
	ProblemDetail handleConflict(InvalidAlertStatusTransitionException exception) {
		return problem(HttpStatus.CONFLICT, "Alert conflict", exception);
	}

	@ExceptionHandler(InvalidAlertException.class)
	ProblemDetail handleInvalid(InvalidAlertException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid alert", exception);
	}

	private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
		detail.setTitle(title);
		return detail;
	}
}
