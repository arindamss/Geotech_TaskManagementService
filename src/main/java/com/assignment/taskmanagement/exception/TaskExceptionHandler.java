package com.assignment.taskmanagement.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class TaskExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handlevalidationException(MethodArgumentNotValidException ex) {
		
		log.info("Method Argument validation exception ouccered");
		log.debug("Method Argument validation exception E: {}", ex.getMessage());
		
		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problemDetail.setTitle("Validation failed");

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (first, second) -> first
                ));

        problemDetail.setProperty("errors", errors);

        return problemDetail;
	}
	
	@ExceptionHandler(TaskNotFoundException.class)
	public ProblemDetail handleNotFound(TaskNotFoundException ex){
		return ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
	}
}
