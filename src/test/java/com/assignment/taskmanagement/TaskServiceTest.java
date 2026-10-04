package com.assignment.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.domain.TaskStatus;
import com.assignment.taskmanagement.dto.response.TaskResponse;
import com.assignment.taskmanagement.exception.InvalidDependencyException;
import com.assignment.taskmanagement.exception.InvalidStatusTransitionException;
import com.assignment.taskmanagement.mapper.TaskMapper;
import com.assignment.taskmanagement.repository.TaskRepository;
import com.assignment.taskmanagement.service.DependencyManagementService;
import com.assignment.taskmanagement.service.TaskService;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
	
	@Mock
	TaskMapper mapper;
	
	@Mock
	TaskRepository repository;
	
	@InjectMocks
	TaskService service;
	
	@Test
	void cannotStartTaskWhileDependencyIsNotDone() {		
		Task a = task(UUID.randomUUID());
		Task b = task(UUID.randomUUID());
		a.getDependencies().add(b);
		
		when(repository.findById(a.getId())).thenReturn(Optional.of(a));
		
		assertThatThrownBy(() -> service.updateStatus(a.getId(), TaskStatus.IN_PROGRESS))
				.isInstanceOf(InvalidStatusTransitionException.class)
				.hasMessageContaining(b.getId().toString());
		
	}
	
	@Test
	void cannotCompleteTaskWhileDependencyIsNotDone() {		
		Task a = task(UUID.randomUUID());
		Task b = task(UUID.randomUUID());
		a.getDependencies().add(b);
		
		when(repository.findById(a.getId())).thenReturn(Optional.of(a));
		
		assertThatThrownBy(() -> service.updateStatus(a.getId(), TaskStatus.DONE))
				.isInstanceOf(InvalidStatusTransitionException.class);
	}
	
	@Test
	void canCompleteTaskOnceDependenciesAreDone() {
		Task a = task(UUID.randomUUID());
		Task b = task(UUID.randomUUID());
		a.getDependencies().add(b);
		b.setStatus(TaskStatus.DONE);
		
		when(repository.findById(a.getId())).thenReturn(Optional.of(a));
		
		service.updateStatus(a.getId(), TaskStatus.DONE);

	    assertThat(a.getStatus()).isEqualTo(TaskStatus.DONE);
	}
	
	
	static Task task(UUID id) {
		Task task = Task.builder().title("Task " + id).description(null).build();
		ReflectionTestUtils.setField(task, "id", id);
		return task;
	}
}
