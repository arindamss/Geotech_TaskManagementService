package com.assignment.taskmanagement.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.dto.response.ExecutionOrderResponse;
import com.assignment.taskmanagement.exception.InvalidDependencyException;

@Service
final class ExecutionOrderResolver {

	public static ExecutionOrderResponse resolve(Collection<Task> tasks){
		

	    Map<UUID, Task> taskMap = tasks.stream()
	            .collect(Collectors.toMap(
	                    Task::getId,
	                    Function.identity()
	            ));

	    Map<UUID, Integer> inDegree = new HashMap<>();
	    Map<UUID, List<UUID>> dependents = new HashMap<>();

	    /*
	     * Initially every task has an in-degree of 0.
	     */
	    for (Task task : tasks) {
	        inDegree.put(task.getId(), 0);
	        dependents.put(task.getId(), new ArrayList<>());
	    }

	    /*
	     * If:
	     *
	     * Task 2 depends on Task 1
	     *
	     * graph:
	     *
	     * Task 1 ---> Task 2
	     *
	     * Therefore Task 2 has in-degree 1.
	     */
	    for (Task task : tasks) {

	        for (Task dependency : task.getDependencies()) {

	            UUID dependencyId = dependency.getId();
	            UUID taskId = task.getId();

	            inDegree.merge(taskId, 1, Integer::sum);

	            dependents
	                    .get(dependencyId)
	                    .add(taskId);
	        }
	    }

	    /*
	     * Tasks with no dependencies can execute first.
	     */
	    Queue<UUID> readyTasks = new ArrayDeque<>();

	    for (Map.Entry<UUID, Integer> entry : inDegree.entrySet()) {

	        if (entry.getValue() == 0) {
	            readyTasks.offer(entry.getKey());
	        }
	    }

	    List<UUID> executionOrder = new ArrayList<>();

	    /*
	     * Kahn's topological sorting algorithm.
	     */
	    while (!readyTasks.isEmpty()) {

	    	UUID currentTaskId = readyTasks.poll();

	        executionOrder.add(currentTaskId);

	        for (UUID dependentTaskId : dependents.get(currentTaskId)) {

	            int newDegree =
	                    inDegree.get(dependentTaskId) - 1;

	            inDegree.put(dependentTaskId, newDegree);

	            if (newDegree == 0) {
	                readyTasks.offer(dependentTaskId);
	            }
	        }
	    }

	    /*
	     * If we couldn't process every task,
	     * there is a circular dependency.
	     */
	    if (executionOrder.size() != tasks.size()) {
	        throw new InvalidDependencyException(
	                "Cannot determine execution order because circular dependency exists"
	        );
	    }

	    return new ExecutionOrderResponse(executionOrder);
	}
}
