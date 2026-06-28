package com.portfolio.mini_utm.mission.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.portfolio.mini_utm.mission.application.DuplicateMissionNameException;
import com.portfolio.mini_utm.mission.application.InvalidMissionException;
import com.portfolio.mini_utm.mission.application.MissionNotFoundException;
import com.portfolio.mini_utm.mission.domain.InvalidMissionStatusTransitionException;

@RestControllerAdvice
public class MissionExceptionHandler {

	@ExceptionHandler(MissionNotFoundException.class)
	ProblemDetail handleNotFound(MissionNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Mission not found", exception);
	}

	@ExceptionHandler({DuplicateMissionNameException.class, InvalidMissionStatusTransitionException.class})
	ProblemDetail handleConflict(RuntimeException exception) {
		return problem(HttpStatus.CONFLICT, "Mission conflict", exception);
	}

	@ExceptionHandler(InvalidMissionException.class)
	ProblemDetail handleInvalid(InvalidMissionException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid mission", exception);
	}

	private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
		detail.setTitle(title);
		return detail;
	}
}
