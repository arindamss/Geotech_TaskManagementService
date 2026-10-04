package com.assignment.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public class InvalidDependencyException extends RuntimeException implements HttpStatusProvider {
	
	private HttpStatus httpStatus = HttpStatus.CONFLICT;
	
	public InvalidDependencyException(String msg) {
		super(msg);
	}
	
	public InvalidDependencyException(String msg, HttpStatus status) {
		super(msg);
		this.httpStatus = status;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return this.httpStatus;
	}

}
