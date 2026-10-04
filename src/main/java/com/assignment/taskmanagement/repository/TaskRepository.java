package com.assignment.taskmanagement.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.assignment.taskmanagement.domain.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>{
	@Query("""
	        SELECT DISTINCT t
	        FROM Task t
	        LEFT JOIN FETCH t.dependencies
	        """)
	List<Task> findAllWithDependencies();
}
