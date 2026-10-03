package com.assignment.taskmanagement.dto.request;

import com.assignment.taskmanagement.domain.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TaskStatus status) {

}
