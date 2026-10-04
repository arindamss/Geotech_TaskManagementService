package com.assignment.taskmanagement.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.HeadersBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assignment.taskmanagement.dto.request.AddDependencyRequest;
import com.assignment.taskmanagement.dto.request.CreateTaskRequest;
import com.assignment.taskmanagement.dto.request.UpdateStatusRequest;
import com.assignment.taskmanagement.dto.response.ExecutionOrderResponse;
import com.assignment.taskmanagement.dto.response.TaskDependencyResponse;
import com.assignment.taskmanagement.dto.response.TaskResponse;
import com.assignment.taskmanagement.service.TaskService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
	
	private final TaskService taskService;
	
	public TaskController(TaskService service) {
		this.taskService = service;
	}

	@PostMapping
	public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
		
		log.debug("Recived task created request, Title: {}", request.title());
		return new ResponseEntity(taskService.createTask(request), HttpStatus.CREATED);
	}
	
	@GetMapping
	public ResponseEntity<List<TaskResponse>> getAllTasks(){
		return new ResponseEntity(taskService.getAllTasks(), HttpStatus.OK);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<TaskResponse> getTaskById(@PathVariable("id") UUID taskId){
		return new ResponseEntity(taskService.getTaskById(taskId), HttpStatus.OK);
	}
	
	@PatchMapping("/{id}/status")
	public ResponseEntity<TaskResponse> updateTaskStatus(@PathVariable("id") UUID taskId, @Valid @RequestBody UpdateStatusRequest request){
		return new ResponseEntity(taskService.updateStatus(taskId, request.status()), HttpStatus.OK);
	}
	
	@PostMapping("/{id}/dependencies")
	public ResponseEntity<TaskResponse> addDependency(@PathVariable("id") UUID taskId, @Valid @RequestBody AddDependencyRequest request){
		return new ResponseEntity(taskService.addDependency(taskId, request.dependencyId()), HttpStatus.OK);
	}
	
	@GetMapping("/{id}/dependencies")
	public ResponseEntity<List<TaskDependencyResponse>> getDependencies(@PathVariable("id") UUID id){
		return new ResponseEntity(taskService.getDependencies(id), HttpStatus.OK);
	}
	
	@DeleteMapping("/{id}/dependencies/{dependsOnId}")
    public HeadersBuilder<?> removeDependency(@PathVariable UUID id, @PathVariable UUID dependsOnId) {
		taskService.removeDependency(id, dependsOnId);
		return ResponseEntity.noContent();
    }
	
	@GetMapping("/execution-order")
    public ResponseEntity<ExecutionOrderResponse> executionOrder() {
        return new ResponseEntity(taskService.executionOrder(), HttpStatus.OK);
    }
}
