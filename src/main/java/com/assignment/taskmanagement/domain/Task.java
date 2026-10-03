package com.assignment.taskmanagement.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(nullable = false, length = 200)
	private String title;
	
	@Column(length = 200)
	private String description;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
    @Builder.Default
	private TaskStatus status = TaskStatus.TODO;
	
	@Column(nullable = false, updatable = false)
	private Instant createdAt;
	
	@ManyToMany
	@JoinTable(
			name = "task_dependencies",
			joinColumns = @JoinColumn(name = "task_id"),
			inverseJoinColumns = @JoinColumn(name = "dependency_id")
	)
	@Builder.Default
	private Set<Task> dependencies = new HashSet();
	
	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}
}
