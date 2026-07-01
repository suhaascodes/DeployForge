package com.deployforge.deployment.service;

import com.deployforge.common.exception.ResourceNotFoundException;
import com.deployforge.deployment.dto.DeploymentCreateRequest;
import com.deployforge.deployment.dto.DeploymentResponse;
import com.deployforge.deployment.entity.*;
import com.deployforge.deployment.repository.DeploymentEventRepository;
import com.deployforge.deployment.repository.DeploymentRepository;
import com.deployforge.deployment.repository.DeploymentRuntimeRepository;
import com.deployforge.deployment.repository.DeploymentVersionRepository;
import com.deployforge.project.entity.Project;
import com.deployforge.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final DeploymentEventRepository deploymentEventRepository;

    public DeploymentService(DeploymentRepository deploymentRepository,
                             DeploymentVersionRepository deploymentVersionRepository,
                             DeploymentRuntimeRepository deploymentRuntimeRepository,
                             ProjectRepository projectRepository,
                             DeploymentQueue deploymentQueue,
                             DeploymentEventRepository deploymentEventRepository) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentVersionRepository = deploymentVersionRepository;
        this.deploymentRuntimeRepository = deploymentRuntimeRepository;
        this.projectRepository = projectRepository;
        this.deploymentQueue = deploymentQueue;
        this.deploymentEventRepository = deploymentEventRepository;
    }

    @Transactional
    public DeploymentResponse createDeployment(DeploymentCreateRequest request, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(request.getProjectId(), ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you are not the owner"));

        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .triggerType(DeploymentTriggerType.MANUAL)
                .build();

        Deployment savedDeployment = deploymentRepository.save(deployment);

        DeploymentEvent eventQueued = DeploymentEvent.builder()
                .deployment(savedDeployment)
                .eventType(DeploymentEventType.QUEUED)
                .message("Manual deployment added to queue")
                .build();
        deploymentEventRepository.save(eventQueued);

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

    @Transactional
    public DeploymentResponse triggerWebhookDeployment(Project project, String commitHash, String commitMsg, String author) {
        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .triggerType(DeploymentTriggerType.GITHUB_WEBHOOK)
                .githubCommitHash(commitHash)
                .githubCommitMessage(commitMsg)
                .githubAuthor(author)
                .githubPushTimestamp(LocalDateTime.now())
                .build();

        Deployment savedDeployment = deploymentRepository.save(deployment);

        DeploymentEvent eventReceived = DeploymentEvent.builder()
                .deployment(savedDeployment)
                .eventType(DeploymentEventType.WEBHOOK_RECEIVED)
                .message("GitHub push webhook received for commit " + (commitHash.length() > 7 ? commitHash.substring(0, 7) : commitHash) + " by " + author)
                .build();
        deploymentEventRepository.save(eventReceived);

        DeploymentEvent eventQueued = DeploymentEvent.builder()
                .deployment(savedDeployment)
                .eventType(DeploymentEventType.QUEUED)
                .message("Webhook deployment added to queue")
                .build();
        deploymentEventRepository.save(eventQueued);

        // Enqueue deployment ID asynchronously
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

    @Transactional
    public DeploymentResponse redeploy(UUID sourceDeploymentId, UUID ownerId) {
        Deployment sourceDep = deploymentRepository.findById(sourceDeploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Source deployment not found"));

        Project project = sourceDep.getProject();
        if (!project.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Project not found or you are not the owner");
        }

        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.QUEUED)
                .triggerType(DeploymentTriggerType.MANUAL)
                .sourceDeploymentId(sourceDeploymentId)
                .githubCommitHash(sourceDep.getGithubCommitHash())
                .githubCommitMessage(sourceDep.getGithubCommitMessage())
                .githubAuthor(sourceDep.getGithubAuthor())
                .githubPushTimestamp(sourceDep.getGithubPushTimestamp())
                .build();

        Deployment savedDeployment = deploymentRepository.save(deployment);

        DeploymentEvent eventQueued = DeploymentEvent.builder()
                .deployment(savedDeployment)
                .eventType(DeploymentEventType.QUEUED)
                .message("Manual redeployment of Run #" + sourceDeploymentId.toString().substring(0, 8) + " queued")
                .build();
        deploymentEventRepository.save(eventQueued);

        // Enqueue deployment ID asynchronously
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
    public List<com.deployforge.deployment.dto.DeploymentEventDto> getDeploymentEvents(UUID deploymentId, UUID ownerId) {
        Deployment dep = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found"));
        if (!dep.getProject().getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Deployment not found or you are not the owner");
        }

        return deploymentEventRepository.findByDeploymentIdOrderByCreatedAtAsc(deploymentId)
                .stream()
                .map(com.deployforge.deployment.dto.DeploymentEventDto::fromEntity)
                .collect(Collectors.toList());
    }
}
