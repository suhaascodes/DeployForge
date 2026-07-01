package com.deployforge.deployment.repository;

import com.deployforge.deployment.entity.DeploymentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentVersionRepository extends JpaRepository<DeploymentVersion, UUID> {
    Optional<DeploymentVersion> findByDeploymentId(UUID deploymentId);
}
