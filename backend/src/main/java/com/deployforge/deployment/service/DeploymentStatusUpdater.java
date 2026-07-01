package com.deployforge.deployment.service;

import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentStatus;
import com.deployforge.deployment.repository.DeploymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DeploymentStatusUpdater {

    private final DeploymentRepository deploymentRepository;

    public DeploymentStatusUpdater(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID id, DeploymentStatus status) {
        updateStatus(id, status, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID id, DeploymentStatus status, String url, String framework) {
        Deployment dep = deploymentRepository.findById(id).orElse(null);
        if (dep != null) {
            dep.setStatus(status);
            if (url != null) {
                dep.setDeploymentUrl(url);
            }
            if (framework != null) {
                dep.setFrameworkDetected(framework);
            }
            
            if (status == DeploymentStatus.BUILDING) {
                dep.setBuildStartedAt(LocalDateTime.now());
            } else if (status == DeploymentStatus.CREATING_IMAGE) {
                dep.setBuildCompletedAt(LocalDateTime.now());
            } else if (status == DeploymentStatus.RUNNING || status == DeploymentStatus.BUILD_FAILED || status == DeploymentStatus.RUNTIME_FAILED) {
                dep.setCompletedAt(LocalDateTime.now());
            }
            
            deploymentRepository.save(dep);
        }
    }
}
