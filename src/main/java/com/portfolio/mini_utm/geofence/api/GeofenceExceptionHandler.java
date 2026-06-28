package com.portfolio.mini_utm.geofence.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.portfolio.mini_utm.geofence.application.DuplicateGeofenceNameException;
import com.portfolio.mini_utm.geofence.application.GeofenceNotFoundException;
import com.portfolio.mini_utm.geofence.application.InvalidGeofenceException;

@RestControllerAdvice
public class GeofenceExceptionHandler {

	@ExceptionHandler(GeofenceNotFoundException.class)
	ProblemDetail handleNotFound(GeofenceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Geofence not found", exception);
	}

	@ExceptionHandler(DuplicateGeofenceNameException.class)
	ProblemDetail handleConflict(DuplicateGeofenceNameException exception) {
		return problem(HttpStatus.CONFLICT, "Geofence conflict", exception);
	}

	@ExceptionHandler(InvalidGeofenceException.class)
	ProblemDetail handleInvalid(InvalidGeofenceException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid geofence", exception);
	}

	private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
		detail.setTitle(title);
		return detail;
	}
}
