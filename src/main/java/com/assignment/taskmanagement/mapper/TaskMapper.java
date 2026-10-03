package com.assignment.taskmanagement.mapper;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.dto.response.TaskResponse;

@Component
public class TaskMapper {
	public TaskResponse toResponse(Task task) {
		Set<UUID> dependencyIds = task.getDependencies().stream()
				.map(Task::getId)
				.collect(Collectors.toSet());
		
		return new TaskResponse(
					task.getId(), 
					task.getTitle(), 
					task.getDescription(), 
					task.getStatus(), 
					task.getCreatedAt(),
					dependencyIds
				);
	}
}
