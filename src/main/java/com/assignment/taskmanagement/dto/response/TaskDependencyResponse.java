package com.assignment.taskmanagement.dto.response;

import java.util.UUID;

import com.assignment.taskmanagement.domain.TaskStatus;

public record TaskDependencyResponse(
			UUID id,
			String title,
			TaskStatus status
		) {

}
