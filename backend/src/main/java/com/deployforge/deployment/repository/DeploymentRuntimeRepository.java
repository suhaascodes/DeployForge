package com.deployforge.deployment.repository;

import com.deployforge.deployment.entity.DeploymentRuntime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentRuntimeRepository extends JpaRepository<DeploymentRuntime, UUID> {
    Optional<DeploymentRuntime> findByDeploymentId(UUID deploymentId);
}
