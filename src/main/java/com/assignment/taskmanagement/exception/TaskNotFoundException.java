package com.assignment.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public class TaskNotFoundException extends RuntimeException implements HttpStatusProvider {
	
	private HttpStatus httpStatus = HttpStatus.NOT_FOUND;
	
	public TaskNotFoundException(String msg) {
		super(msg);
	}
	
	public TaskNotFoundException(String msg, HttpStatus httpStatus) {
		super(msg);
		this.httpStatus = httpStatus;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return this.httpStatus;
	}

}
