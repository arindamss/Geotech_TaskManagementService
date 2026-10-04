package com.assignment.taskmanagement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.domain.TaskStatus;
import com.assignment.taskmanagement.dto.request.CreateTaskRequest;
import com.assignment.taskmanagement.dto.response.TaskResponse;
import com.assignment.taskmanagement.exception.InvalidDependencyException;
import com.assignment.taskmanagement.exception.InvalidStatusTransitionException;
import com.assignment.taskmanagement.exception.TaskNotFoundException;
import com.assignment.taskmanagement.mapper.TaskMapper;
import com.assignment.taskmanagement.repository.TaskRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TaskService {
	
	private final TaskMapper mapper;
	private final TaskRepository taskRepository;
	private final DependencyManagementService dependencyManagementService;
	
	public TaskService(TaskMapper mapper, TaskRepository repository, DependencyManagementService dependencyManagementService) {
		this.mapper = mapper;
		this.taskRepository = repository;
		this.dependencyManagementService = dependencyManagementService;
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
	
	
	public List<TaskResponse> getAllTasks(){
		return taskRepository.findAll()
				.stream().map(mapper::toResponse).toList();
	}
	
	
	public TaskResponse getTaskById(UUID id) {
		return mapper.toResponse(getTask(id));
	}
	
	
	@Transactional
	public TaskResponse updateStatus(UUID id, TaskStatus newStatus) {
		Task task = getTask(id);
		
		if(newStatus != TaskStatus.TODO && task.isBlocked()) {
			log.debug("Exception while task status updation: {}", id);
			
			List<UUID> blockers = task.blockingTasks().stream().map(Task::getId).toList();
			throw new InvalidStatusTransitionException("Task %s cannot move to %s: blocked by unfinished tasks %s".formatted(id, newStatus, blockers));
		}
		
		task.setStatus(newStatus);
		log.info("Task status updated for: {}", id);
		
		return mapper.toResponse(task);
	}
	
	
	@Transactional
	public TaskResponse addDependency(UUID takId, UUID dependencyId) {
		Task task = getTask(takId);
		
		Task dependency = getTask(dependencyId);
		
		dependencyManagementService.validateDependency(task, dependency);
		
		task.getDependencies().add(dependency);
		
		Task savedTask = taskRepository.save(task);
		log.debug("Dependency {} added to task {}", dependencyId, takId);
		
		return mapper.toResponse(savedTask);
	}
	
	private Task getTask(UUID id) {
		return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException("Task not found: "+id));
	}
	
}
