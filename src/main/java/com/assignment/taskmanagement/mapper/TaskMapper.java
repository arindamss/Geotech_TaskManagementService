package com.assignment.taskmanagement.mapper;

import org.springframework.stereotype.Component;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.dto.response.TaskResponse;

@Component
public class TaskMapper {
	public TaskResponse toResponse(Task task) {
		return new TaskResponse(
					task.getId(), 
					task.getTitle(), 
					task.getDescription(), 
					task.getStatus(), 
					task.getCreatedAt()
				);
	}
}
