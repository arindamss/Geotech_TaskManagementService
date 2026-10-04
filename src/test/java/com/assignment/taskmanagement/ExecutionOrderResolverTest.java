package com.assignment.taskmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.exception.InvalidDependencyException;
import com.assignment.taskmanagement.service.ExecutionOrderResolver;

@ExtendWith(MockitoExtension.class)
public class ExecutionOrderResolverTest {

	@Test
	public void placesDependenciesBeforeDependents() {
		Task t1 = newTask();
		Task t2 = newTask();
		Task t3 = newTask();
		Task standalone = newTask();
		
		t2.getDependencies().add(t1);
		t3.getDependencies().add(t2);
		
		List<UUID> order = ExecutionOrderResolver.resolve(List.of(t3, standalone, t2, t1)).executionOrder();
		
		assertThat(order).containsExactlyInAnyOrder(t1.getId(), t2.getId(), t3.getId(), standalone.getId());
		assertRunsBefore(order, t1, t2);
		assertRunsBefore(order, t2, t3);
	}
	
	
	@Test
	void handlesDiamondShapedDependencies() {
		Task a = newTask();
        Task b = newTask();
        Task c = newTask();
        Task d = newTask();
        b.getDependencies().add(a);
        c.getDependencies().add(a);
        d.getDependencies().add(b);
        d.getDependencies().add(c);
        
        List<UUID> order = ExecutionOrderResolver.resolve(List.of(d, c, b, a)).executionOrder();
        
        assertThat(order).hasSize(4);
        assertRunsBefore(order, a, b);
        assertRunsBefore(order, a, c);
        assertRunsBefore(order, b, d);
        assertRunsBefore(order, c, d);
	}
	
	
	@Test
    void throwsWhenCircularDependencyExists() {
        Task a = newTask();
        Task b = newTask();
        a.getDependencies().add(b);
        b.getDependencies().add(a);

        assertThatThrownBy(() -> ExecutionOrderResolver.resolve(List.of(a, b)))
                .isInstanceOf(InvalidDependencyException.class)
                .hasMessageContaining("circular");
    }
	
	
	private static Task newTask() {
		return TaskServiceTest.task(UUID.randomUUID());
	}
	
	private static void assertRunsBefore(List<UUID> order, Task first, Task second) {
        assertThat(order.indexOf(first.getId()))
                .as("task should run before its dependent")
                .isLessThan(order.indexOf(second.getId()));
    }
}
