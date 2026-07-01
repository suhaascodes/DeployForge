package com.deployforge.deployment.service;

import com.deployforge.deployment.dto.WebSocketStatusMessage;
import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentEvent;
import com.deployforge.deployment.entity.DeploymentEventType;
import com.deployforge.deployment.entity.DeploymentStatus;
import com.deployforge.deployment.repository.DeploymentEventRepository;
import com.deployforge.deployment.repository.DeploymentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DeploymentStatusUpdater {

    private final DeploymentRepository deploymentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeploymentEventRepository deploymentEventRepository;

    public DeploymentStatusUpdater(DeploymentRepository deploymentRepository,
                                   @Lazy SimpMessagingTemplate messagingTemplate,
                                   DeploymentEventRepository deploymentEventRepository) {
        this.deploymentRepository = deploymentRepository;
        this.messagingTemplate = messagingTemplate;
        this.deploymentEventRepository = deploymentEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID id, DeploymentStatus status) {
        updateStatus(id, status, null, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID id, DeploymentStatus status, String url, String framework) {
        updateStatus(id, status, url, framework, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID id, DeploymentStatus status, String url, String framework, String failureStage, String failureSummary) {
        Deployment dep = deploymentRepository.findById(id).orElse(null);
        if (dep != null) {
            dep.setStatus(status);
            if (url != null) {
                dep.setDeploymentUrl(url);
            }
            if (framework != null) {
                dep.setFrameworkDetected(framework);
            }
            if (failureStage != null) {
                dep.setFailureStage(failureStage);
            }
            if (failureSummary != null) {
                dep.setFailureSummary(failureSummary);
            }

            if (status == DeploymentStatus.BUILDING) {
                dep.setBuildStartedAt(LocalDateTime.now());
            } else if (status == DeploymentStatus.CREATING_IMAGE) {
                dep.setBuildCompletedAt(LocalDateTime.now());
            } else if (status == DeploymentStatus.RUNNING || status == DeploymentStatus.BUILD_FAILED || status == DeploymentStatus.RUNTIME_FAILED) {
                dep.setCompletedAt(LocalDateTime.now());
            }

            deploymentRepository.save(dep);

            // Record corresponding events
            recordTimelineEvents(dep, status);

            // Broadcast real-time status update to client
            broadcastStatus(id, status);
        }
    }

    private void recordTimelineEvents(Deployment dep, DeploymentStatus status) {
        try {
            switch (status) {
                case QUEUED:
                    recordEvent(dep, DeploymentEventType.QUEUED, "Deployment added to task queue");
                    break;
                case CLONING:
                    recordEvent(dep, DeploymentEventType.CLONING, "Starting git clone of repository source files");
                    break;
                case BUILDING:
                    recordEvent(dep, DeploymentEventType.BUILD_STARTED, "Triggering project compiler build step");
                    break;
                case CREATING_IMAGE:
                    recordEvent(dep, DeploymentEventType.BUILD_COMPLETED, "Project compile finished. Initiating Docker image build.");
                    break;
                case STARTING_CONTAINER:
                    recordEvent(dep, DeploymentEventType.IMAGE_CREATED, "Docker container image built and stored.");
                    recordEvent(dep, DeploymentEventType.CONTAINER_STARTED, "Spinning up runtime container on allocated host port.");
                    break;
                case RUNNING:
                    recordEvent(dep, DeploymentEventType.HEALTHCHECK_PASSED, "HTTP health check completed successfully.");
                    recordEvent(dep, DeploymentEventType.DEPLOYMENT_READY, "Deployment ready and running live at: " + dep.getDeploymentUrl());
                    break;
                case BUILD_FAILED:
                case RUNTIME_FAILED:
                    String summary = dep.getFailureSummary() != null ? dep.getFailureSummary() : "An unexpected error occurred.";
                    recordEvent(dep, DeploymentEventType.DEPLOYMENT_FAILED, "Pipeline failed during stage: " + dep.getFailureStage() + ". Detail: " + summary);
                    break;
            }
        } catch (Exception e) {
            // Safeguard to prevent logging failures from aborting status changes
        }
    }

    private void recordEvent(Deployment dep, DeploymentEventType type, String message) {
        DeploymentEvent event = DeploymentEvent.builder()
                .deployment(dep)
                .eventType(type)
                .message(message)
                .build();
        deploymentEventRepository.save(event);
    }

    private void broadcastStatus(UUID id, DeploymentStatus status) {
        try {
            WebSocketStatusMessage msg = WebSocketStatusMessage.builder()
                    .type("STATUS")
                    .status(status.name())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            messagingTemplate.convertAndSend("/topic/deployments/" + id + "/status", msg);
        } catch (Exception e) {
            // Safeguard
        }
    }
}
