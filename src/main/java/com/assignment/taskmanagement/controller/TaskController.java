package com.assignment.taskmanagement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assignment.taskmanagement.dto.request.CreateTaskRequest;
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
}
