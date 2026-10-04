package com.assignment.taskmanagement.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddDependencyRequest(
			@NotNull
			UUID dependencyId
		) {}
