package com.deployforge.project.repository;

import com.deployforge.project.entity.ProjectEnvironmentVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnvironmentVariableRepository extends JpaRepository<ProjectEnvironmentVariable, UUID> {
    List<ProjectEnvironmentVariable> findByProjectIdOrderByKeyAsc(UUID projectId);
    Optional<ProjectEnvironmentVariable> findByProjectIdAndKey(UUID projectId, String key);
}
