package com.assignment.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends RuntimeException implements HttpStatusProvider {
	
	private HttpStatus httpStatus = HttpStatus.CONFLICT;
	
	public InvalidStatusTransitionException(String msg) {
		super(msg);
	}
	
	public InvalidStatusTransitionException(String msg, HttpStatus status) {
		super(msg);
		this.httpStatus = status;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return this.httpStatus;
	}

}
