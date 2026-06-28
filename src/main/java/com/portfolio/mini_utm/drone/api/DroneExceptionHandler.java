package com.portfolio.mini_utm.drone.api;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.portfolio.mini_utm.drone.application.DroneNotFoundException;
import com.portfolio.mini_utm.drone.application.DuplicateDroneSerialNumberException;
import com.portfolio.mini_utm.drone.domain.InvalidDroneStatusTransitionException;

@RestControllerAdvice
public class DroneExceptionHandler {

	@ExceptionHandler(DroneNotFoundException.class)
	ProblemDetail handleNotFound(DroneNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Drone not found", exception);
	}

	@ExceptionHandler({DuplicateDroneSerialNumberException.class, InvalidDroneStatusTransitionException.class})
	ProblemDetail handleConflict(RuntimeException exception) {
		return problem(HttpStatus.CONFLICT, "Drone conflict", exception);
	}

	private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
		detail.setTitle(title);
		detail.setType(URI.create("about:blank"));
		return detail;
	}
}
