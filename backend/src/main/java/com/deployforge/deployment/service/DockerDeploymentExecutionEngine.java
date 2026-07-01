package com.deployforge.deployment.service;

import com.deployforge.deployment.build.BuildExecutionService;
import com.deployforge.deployment.docker.DockerBuildService;
import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentRuntime;
import com.deployforge.deployment.entity.DeploymentStatus;
import com.deployforge.deployment.entity.ProjectType;
import com.deployforge.deployment.repository.DeploymentRepository;
import com.deployforge.deployment.repository.RepositoryCloneService;
import com.deployforge.deployment.runtime.ContainerCleanupService;
import com.deployforge.deployment.runtime.ContainerRuntimeService;
import com.deployforge.logging.entity.LogCategory;
import com.deployforge.logging.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

@Service
@Slf4j
public class DockerDeploymentExecutionEngine implements DeploymentExecutionEngine {

    private final DeploymentRepository deploymentRepository;
    private final RepositoryCloneService repositoryCloneService;
    private final ProjectTypeDetector projectTypeDetector;
    private final BuildExecutionService buildExecutionService;
    private final DockerBuildService dockerBuildService;
    private final ContainerRuntimeService containerRuntimeService;
    private final ContainerCleanupService containerCleanupService;
    private final WorkspaceCleanupService workspaceCleanupService;
    private final PreviewUrlGenerator previewUrlGenerator;
    private final LogService logService;
    private final DeploymentStatusUpdater deploymentStatusUpdater;
    private final com.deployforge.project.repository.EnvironmentVariableRepository environmentVariableRepository;
    private final com.deployforge.config.EncryptionService encryptionService;
    private final MetricsCollectionService metricsCollectionService;

    public DockerDeploymentExecutionEngine(
            DeploymentRepository deploymentRepository,
            RepositoryCloneService repositoryCloneService,
            ProjectTypeDetector projectTypeDetector,
            BuildExecutionService buildExecutionService,
            DockerBuildService dockerBuildService,
            ContainerRuntimeService containerRuntimeService,
            ContainerCleanupService containerCleanupService,
            WorkspaceCleanupService workspaceCleanupService,
            PreviewUrlGenerator previewUrlGenerator,
            LogService logService,
            DeploymentStatusUpdater deploymentStatusUpdater,
            com.deployforge.project.repository.EnvironmentVariableRepository environmentVariableRepository,
            com.deployforge.config.EncryptionService encryptionService,
            MetricsCollectionService metricsCollectionService) {
        this.deploymentRepository = deploymentRepository;
        this.repositoryCloneService = repositoryCloneService;
        this.projectTypeDetector = projectTypeDetector;
        this.buildExecutionService = buildExecutionService;
        this.dockerBuildService = dockerBuildService;
        this.containerRuntimeService = containerRuntimeService;
        this.containerCleanupService = containerCleanupService;
        this.workspaceCleanupService = workspaceCleanupService;
        this.previewUrlGenerator = previewUrlGenerator;
        this.logService = logService;
        this.deploymentStatusUpdater = deploymentStatusUpdater;
        this.environmentVariableRepository = environmentVariableRepository;
        this.encryptionService = encryptionService;
        this.metricsCollectionService = metricsCollectionService;
    }

    @Override
    public void execute(UUID deploymentId) {
        log.info("Execution engine running deployment task: {}", deploymentId);
        long startTime = System.currentTimeMillis();

        // Fetch deployment details (eagerly resolving project/owner associations)
        Deployment deployment = deploymentRepository.findByIdWithProjectAndOwner(deploymentId).orElse(null);
        if (deployment == null) {
            log.error("Could not find deployment details for execution: {}", deploymentId);
            return;
        }

        UUID projectId = deployment.getProject().getId();
        File workspaceDir = null;
        String currentStage = "CLONING";

        try {
            // 1. Stop and remove previous container instances for this project (redeployments check)
            logService.info(deploymentId.toString(), "Verifying previous project containers...", LogCategory.DEPLOYMENT);
            containerCleanupService.cleanupPreviousProjectRuntimes(projectId, deploymentId);

            // 2. Clone Repository
            logService.info(deploymentId.toString(), "Enqueued deployment. Starting git pull...", LogCategory.DEPLOYMENT);
            deploymentStatusUpdater.updateStatus(deploymentId, DeploymentStatus.CLONING);

            workspaceDir = repositoryCloneService.cloneRepository(deployment);

            // 3. Framework Detection
            logService.info(deploymentId.toString(), "Analyzing repository framework...", LogCategory.DEPLOYMENT);
            ProjectType type = projectTypeDetector.detectProjectType(workspaceDir);
            
            if (type == ProjectType.UNKNOWN) {
                logService.error(deploymentId.toString(), "Unsupported project framework detected (UNKNOWN). Rejecting build.", LogCategory.DEPLOYMENT);
                throw new IllegalArgumentException("Unsupported project framework. React, Spring Boot, or Node.js required.");
            }

            // 4. Build Execution
            currentStage = "BUILD";
            deploymentStatusUpdater.updateStatus(deploymentId, DeploymentStatus.BUILDING, null, type.name());

            buildExecutionService.executeBuild(deploymentId.toString(), workspaceDir, type);

            // 5. Generate Dockerfile and Compile Image
            currentStage = "IMAGE";
            deploymentStatusUpdater.updateStatus(deploymentId, DeploymentStatus.CREATING_IMAGE);

            dockerBuildService.buildImage(deploymentId.toString(), projectId.toString(), workspaceDir, type);

            // Fetch decrypted project environment variables
            java.util.List<String> envVars = new java.util.ArrayList<>();
            for (com.deployforge.project.entity.ProjectEnvironmentVariable env : environmentVariableRepository.findByProjectIdOrderByKeyAsc(projectId)) {
                String decrypted = encryptionService.decrypt(env.getEncryptedValue());
                envVars.add(env.getKey() + "=" + decrypted);
            }

            // 6. Start Container & Network Mapping
            currentStage = "RUNTIME";
            deploymentStatusUpdater.updateStatus(deploymentId, DeploymentStatus.STARTING_CONTAINER);

            // Need to reload the refreshed deployment entity to get correct timestamps/states
            Deployment refreshedDeployment = deploymentRepository.findById(deploymentId).orElse(deployment);
            DeploymentRuntime runtime = containerRuntimeService.startContainer(refreshedDeployment, type, envVars);

            // 7. Generate preview URL and complete deployment
            String previewUrl = previewUrlGenerator.generatePreviewUrl(deploymentId, runtime.getHostPort());
            
            // Calculate static metrics
            long durationMs = System.currentTimeMillis() - startTime;
            deployment.setDeploymentDurationMs(durationMs);
            String imageName = "deployforge-" + projectId.toString().toLowerCase();
            String imageTag = "dep-" + deploymentId.toString().toLowerCase();
            Double sizeMb = metricsCollectionService.getImageSizeMb(imageName, imageTag);
            deployment.setImageSizeMb(sizeMb);
            deploymentRepository.save(deployment);

            deploymentStatusUpdater.updateStatus(deploymentId, DeploymentStatus.RUNNING, previewUrl, null);

            logService.info(deploymentId.toString(), "Deployment completed successfully! Live URL: " + previewUrl, LogCategory.DEPLOYMENT);

        } catch (Exception e) {
            log.error("Deployment failed for task: {}", deploymentId, e);
            String failureSummary = e.getMessage() != null ? e.getMessage() : "An unexpected execution error occurred";
            logService.error(deploymentId.toString(), "Critical failure during stage " + currentStage + ": " + failureSummary, LogCategory.DEPLOYMENT);

            // Check if build completed to segregate build vs runtime failures
            boolean buildCompleted = deploymentRepository.findById(deploymentId)
                    .map(d -> d.getBuildCompletedAt() != null)
                    .orElse(false);

            if (!buildCompleted) {
                deploymentStatusUpdater.updateStatus(deploymentId, DeploymentStatus.BUILD_FAILED, null, null, currentStage, failureSummary);
            } else {
                deploymentStatusUpdater.updateStatus(deploymentId, DeploymentStatus.RUNTIME_FAILED, null, null, currentStage, failureSummary);
            }

            // Cleanup any container states
            try {
                containerCleanupService.cleanupContainer(deploymentId);
            } catch (Exception ex) {
                log.error("Failed to clean up container on error fallback", ex);
            }

        } finally {
            // Wipes clone workspaces recursive
            if (workspaceDir != null) {
                try {
                    workspaceCleanupService.cleanupWorkspace(deploymentId);
                } catch (Exception ex) {
                    log.error("Failed to wipe workspace directory on completion", ex);
                }
            }
        }
    }
}
