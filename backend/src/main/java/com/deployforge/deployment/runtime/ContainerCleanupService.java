package com.deployforge.deployment.runtime;

import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentRuntime;
import com.deployforge.deployment.entity.RuntimeStatus;
import com.deployforge.deployment.repository.DeploymentRepository;
import com.deployforge.deployment.repository.DeploymentRuntimeRepository;
import com.deployforge.logging.entity.LogCategory;
import com.deployforge.logging.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ContainerCleanupService {

    private final DeploymentRuntimeRepository deploymentRuntimeRepository;
    private final DeploymentRepository deploymentRepository;
    private final LogService logService;

    public ContainerCleanupService(DeploymentRuntimeRepository deploymentRuntimeRepository,
                                   @Lazy DeploymentRepository deploymentRepository,
                                   LogService logService) {
        this.deploymentRuntimeRepository = deploymentRuntimeRepository;
        this.deploymentRepository = deploymentRepository;
        this.logService = logService;
    }

    public void cleanupContainer(UUID deploymentId) {
        Optional<DeploymentRuntime> runtimeOpt = deploymentRuntimeRepository.findByDeploymentId(deploymentId);
        if (runtimeOpt.isPresent()) {
            DeploymentRuntime runtime = runtimeOpt.get();
            if (runtime.getRuntimeStatus() != RuntimeStatus.REMOVED) {
                log.info("Cleaning up container: {} for deployment: {}", runtime.getContainerName(), deploymentId);
                logService.info(deploymentId.toString(), "Tearing down container: " + runtime.getContainerName(), LogCategory.RUNTIME);
                
                stopAndRemoveContainer(deploymentId.toString(), runtime.getContainerName());
                
                runtime.setRuntimeStatus(RuntimeStatus.REMOVED);
                runtime.setStoppedAt(LocalDateTime.now());
                deploymentRuntimeRepository.save(runtime);
            }
        }
    }

    public void cleanupPreviousProjectRuntimes(UUID projectId, UUID currentDeploymentId) {
        log.info("Checking previous project containers to stop for project: {}", projectId);
        List<Deployment> deployments = deploymentRepository.findByProjectIdOrderByStartedAtDesc(projectId);
        for (Deployment dep : deployments) {
            if (!dep.getId().equals(currentDeploymentId)) {
                cleanupContainer(dep.getId());
            }
        }
    }

    private void stopAndRemoveContainer(String deploymentId, String containerName) {
        if (containerName == null || containerName.isBlank()) return;

        // docker rm -f containerName
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "rm", "-f", containerName);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[docker rm] {}", line);
                }
            }

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.warn("Docker container removal timed out for: {}", containerName);
                logService.warn(deploymentId, "Docker removal timed out for: " + containerName, LogCategory.RUNTIME);
            }
        } catch (Exception e) {
            log.error("Failed to stop and remove Docker container: {}", containerName, e);
            logService.error(deploymentId, "Failed to stop/remove container: " + e.getMessage(), LogCategory.RUNTIME);
        }
    }
}
