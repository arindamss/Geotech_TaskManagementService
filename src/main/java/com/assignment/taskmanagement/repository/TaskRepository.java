package com.assignment.taskmanagement.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.assignment.taskmanagement.domain.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>{

}
