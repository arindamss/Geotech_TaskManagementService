package com.assignment.taskmanagement.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.assignment.taskmanagement.domain.TaskStatus;

public record TaskResponse(
		UUID id,
		String title,
		String description,
		TaskStatus status,
		Instant createdAt
	) {

}
