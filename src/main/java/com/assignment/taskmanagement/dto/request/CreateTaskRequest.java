package com.assignment.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
		
		@NotBlank(message = "Title must not be blank")
		@Size(max = 200, message = "Title must not exceed 200 characters")
		String title,
		
		@Size(max = 2000, message = "Description must not exceed 2000 characters")
		String description
	) {

}
