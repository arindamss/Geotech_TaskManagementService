package com.assignment.taskmanagement.exception;

import org.springframework.http.HttpStatus;

public interface HttpStatusProvider {
	public HttpStatus getHttpStatus();
}
