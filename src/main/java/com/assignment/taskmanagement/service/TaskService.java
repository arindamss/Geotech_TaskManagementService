package com.assignment.taskmanagement.service;

import org.springframework.stereotype.Service;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.dto.request.CreateTaskRequest;
import com.assignment.taskmanagement.dto.response.TaskResponse;
import com.assignment.taskmanagement.mapper.TaskMapper;
import com.assignment.taskmanagement.repository.TaskRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TaskService {
	
	private final TaskMapper mapper;
	private final TaskRepository taskRepository;
	
	public TaskService(TaskMapper mapper, TaskRepository repository) {
		this.mapper = mapper;
		this.taskRepository = repository;
	}
	
	@Transactional
	public TaskResponse createTask(CreateTaskRequest request) {
		
		Task task = Task.builder()
					.title(request.title())
					.description(request.description())
				.build();
		
		Task savedTask = taskRepository.save(task);
		
		log.info("Task created with Id: {}", task.getId());
		return mapper.toResponse(savedTask);
	}
}
