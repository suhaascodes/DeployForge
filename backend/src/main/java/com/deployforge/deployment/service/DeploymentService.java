package com.deployforge.deployment.service;

import com.deployforge.common.exception.ResourceNotFoundException;
import com.deployforge.deployment.dto.DeploymentCreateRequest;
import com.deployforge.deployment.dto.DeploymentResponse;
import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentRuntime;
import com.deployforge.deployment.entity.DeploymentStatus;
import com.deployforge.deployment.entity.DeploymentVersion;
import com.deployforge.deployment.repository.DeploymentRepository;
import com.deployforge.deployment.repository.DeploymentRuntimeRepository;
import com.deployforge.deployment.repository.DeploymentVersionRepository;
import com.deployforge.project.entity.Project;
import com.deployforge.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final DeploymentVersionRepository deploymentVersionRepository;
    private final DeploymentRuntimeRepository deploymentRuntimeRepository;
    private final ProjectRepository projectRepository;
    private final DeploymentQueue deploymentQueue;

    public DeploymentService(DeploymentRepository deploymentRepository,
                             DeploymentVersionRepository deploymentVersionRepository,
                             DeploymentRuntimeRepository deploymentRuntimeRepository,
                             ProjectRepository projectRepository,
                             DeploymentQueue deploymentQueue) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentVersionRepository = deploymentVersionRepository;
        this.deploymentRuntimeRepository = deploymentRuntimeRepository;
        this.projectRepository = projectRepository;
        this.deploymentQueue = deploymentQueue;
    }

    @Transactional
    public DeploymentResponse createDeployment(DeploymentCreateRequest request, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(request.getProjectId(), ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));

        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .build();

        Deployment savedDeployment = deploymentRepository.save(deployment);

        // Enqueue deployment ID asynchronously after transaction commit
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deploymentQueue.enqueue(savedDeployment.getId());
                    }
                }
            );
        } else {
            deploymentQueue.enqueue(savedDeployment.getId());
        }

        return DeploymentResponse.fromEntity(savedDeployment, null, null);
    }

    @Transactional(readOnly = true)
    public DeploymentResponse getDeployment(UUID deploymentId, UUID ownerId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found"));

        if (!deployment.getProject().getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Deployment not found or you are not the owner");
        }

        DeploymentVersion version = deploymentVersionRepository.findByDeploymentId(deploymentId).orElse(null);
        DeploymentRuntime runtime = deploymentRuntimeRepository.findByDeploymentId(deploymentId).orElse(null);
        return DeploymentResponse.fromEntity(deployment, version, runtime);
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> listDeploymentsForProject(UUID projectId, UUID ownerId) {
        // Verify project ownership
        projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));

        return deploymentRepository.findByProjectIdOrderByStartedAtDesc(projectId)
                .stream()
                .map(d -> {
                    DeploymentVersion version = deploymentVersionRepository.findByDeploymentId(d.getId()).orElse(null);
                    DeploymentRuntime runtime = deploymentRuntimeRepository.findByDeploymentId(d.getId()).orElse(null);
                    return DeploymentResponse.fromEntity(d, version, runtime);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> listRecentDeployments(UUID ownerId) {
        return deploymentRepository.findByOwnerIdOrderByStartedAtDesc(ownerId)
                .stream()
                .limit(10) // Limit to 10 recent deployments
                .map(d -> {
                    DeploymentVersion version = deploymentVersionRepository.findByDeploymentId(d.getId()).orElse(null);
                    DeploymentRuntime runtime = deploymentRuntimeRepository.findByDeploymentId(d.getId()).orElse(null);
                    return DeploymentResponse.fromEntity(d, version, runtime);
                })
                .collect(Collectors.toList());
    }
}
