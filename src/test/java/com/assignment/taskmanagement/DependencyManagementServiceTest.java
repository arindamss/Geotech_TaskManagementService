package com.assignment.taskmanagement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.exception.InvalidDependencyException;
import com.assignment.taskmanagement.service.DependencyManagementService;

@ExtendWith(MockitoExtension.class)
public class DependencyManagementServiceTest {
	
	private final DependencyManagementService validator = new DependencyManagementService();
	
	@Test
    void rejectsIndirectCircularDependency() {
        Task a = TaskServiceTest.task(UUID.randomUUID());
        Task b = TaskServiceTest.task(UUID.randomUUID());
        Task c = TaskServiceTest.task(UUID.randomUUID());
        a.getDependencies().add(b);
        b.getDependencies().add(c);

        assertThatThrownBy(() -> validator.validateDependency(c, a))
                .isInstanceOf(InvalidDependencyException.class)
                .hasMessageContaining("circular");
    }
	
	@Test
	void allowsRedundantButNonCircularDependency() {
		Task a = TaskServiceTest.task(UUID.randomUUID());
        Task b = TaskServiceTest.task(UUID.randomUUID());
        Task c = TaskServiceTest.task(UUID.randomUUID());
        a.getDependencies().add(b);
        b.getDependencies().add(c);
        
        assertThatCode(() -> validator.validateDependency(a, c)).doesNotThrowAnyException();
	}
	
	@Test
	void rejectsSelfDependency() {
		Task a = TaskServiceTest.task(UUID.randomUUID());
		
		assertThatThrownBy(() -> validator.validateDependency(a, a)).isInstanceOf(InvalidDependencyException.class);
	}
	
	@Test
    void rejectsDuplicateDependency() {
        Task a = TaskServiceTest.task(UUID.randomUUID());
        Task b = TaskServiceTest.task(UUID.randomUUID());
        a.getDependencies().add(b);

        assertThatThrownBy(() -> validator.validateDependency(a, b)).isInstanceOf(InvalidDependencyException.class);
    }
}
