package com.assignment.taskmanagement.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.assignment.taskmanagement.domain.Task;
import com.assignment.taskmanagement.exception.InvalidDependencyException;

@Service
public class DependencyManagementService {

	public void validateDependency(Task task, Task dependency) {
		if(task.getId().equals(dependency.getId())) {
			throw new InvalidDependencyException("A task cannot depend on itself");
		}
		
		if(task.getDependencies().contains(dependency)) {
			throw new InvalidDependencyException("Task already depends on task " + dependency.getId());
		}
		
		if(createsCircularDependency(task, dependency)) {
			throw new InvalidDependencyException("Adding this dependency would create a circular dependency");
		}
		
	}
	
	
	private boolean createsCircularDependency(Task task, Task dependency) {
		return hasDependency(dependency, task.getId(), new HashSet<>());
	}
	
	private boolean hasDependency(Task current, UUID targetTaskId, Set<UUID> visited) {
		if(!visited.add(current.getId())) {
			return false;
		}
		
		if(current.getDependencies().stream().anyMatch(dependency -> dependency.getId().equals(targetTaskId))) {
			return true;
		}
		
		return current.getDependencies().stream().anyMatch(dependency ->
																hasDependency(dependency, targetTaskId, visited));
	}
}
