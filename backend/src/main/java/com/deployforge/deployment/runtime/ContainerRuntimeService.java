package com.deployforge.deployment.runtime;

import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentRuntime;
import com.deployforge.deployment.entity.ProjectType;
import com.deployforge.deployment.entity.RuntimeStatus;
import com.deployforge.deployment.repository.DeploymentRuntimeRepository;
import com.deployforge.logging.entity.LogCategory;
import com.deployforge.logging.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ContainerRuntimeService {

    private final DeploymentRuntimeRepository deploymentRuntimeRepository;
    private final LogService logService;

    public ContainerRuntimeService(DeploymentRuntimeRepository deploymentRuntimeRepository, LogService logService) {
        this.deploymentRuntimeRepository = deploymentRuntimeRepository;
        this.logService = logService;
    }

    public DeploymentRuntime startContainer(Deployment deployment, ProjectType projectType) throws Exception {
        UUID deploymentId = deployment.getId();
        UUID projectId = deployment.getProject().getId();

        logService.info(deploymentId.toString(), "Selecting host port dynamically...", LogCategory.RUNTIME);
        int hostPort = findFreePort();
        int containerPort = getContainerPort(projectType);

        String imageName = "deployforge-" + projectId.toString().toLowerCase();
        String imageTag = "dep-" + deploymentId.toString().toLowerCase();
        String containerName = "deployforge-dep-" + deploymentId.toString().toLowerCase();

        logService.info(deploymentId.toString(), "Starting Docker container: " + containerName + " (Exposing Port: " + hostPort + ")", LogCategory.RUNTIME);

        String containerId = runDockerContainer(deploymentId.toString(), imageName, imageTag, containerName, hostPort, containerPort);
        logService.info(deploymentId.toString(), "Container launched with ID: " + containerId.substring(0, 12), LogCategory.RUNTIME);

        // Perform active HTTP connection checks (up to 45 seconds)
        verifyHealth(deploymentId.toString(), hostPort);

        // Build and save the runtime record
        DeploymentRuntime runtime = DeploymentRuntime.builder()
                .deploymentId(deploymentId)
                .containerId(containerId)
                .containerName(containerName)
                .imageTag(imageTag)
                .hostPort(hostPort)
                .runtimeStatus(RuntimeStatus.RUNNING)
                .imageCreatedAt(LocalDateTime.now())
                .build();

        return deploymentRuntimeRepository.save(runtime);
    }

    private int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private int getContainerPort(ProjectType projectType) {
        switch (projectType) {
            case REACT:
            case STATIC_HTML:
                return 80;
            case SPRING_BOOT:
                return 8080;
            case NODE:
                return 3000;
            default:
                throw new IllegalArgumentException("Unsupported project framework for port assignment: " + projectType);
        }
    }

    private String runDockerContainer(String deploymentId, String imageName, String imageTag, String containerName, int hostPort, int containerPort) throws Exception {
        String fullTag = imageName + ":" + imageTag;
        
        // docker run -d -p hostPort:containerPort --name containerName imageName:imageTag
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "-d",
                "-p", hostPort + ":" + containerPort,
                "--name", containerName,
                fullTag
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logService.info(deploymentId, line, LogCategory.RUNTIME);
            }
        }

        boolean completed = process.waitFor(45, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            logService.error(deploymentId, "Docker run command timed out.", LogCategory.RUNTIME);
            throw new RuntimeException("Docker run command timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            logService.error(deploymentId, "Docker run failed with exit code: " + exitCode, LogCategory.RUNTIME);
            throw new RuntimeException("Docker run failed with exit code: " + exitCode);
        }

        // The stdout of "docker run -d" is the full container ID
        return output.toString().trim();
    }

    private void verifyHealth(String deploymentId, int port) throws Exception {
        log.info("Starting health checks for container on port: {}", port);
        logService.info(deploymentId, "Awaiting application startup. Polling connection on http://host.docker.internal:" + port + "...", LogCategory.RUNTIME);

        int maxAttempts = 30;
        int attempt = 1;
        String urlString = "http://host.docker.internal:" + port;

        while (attempt <= maxAttempts) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");
                
                int code = connection.getResponseCode();
                log.info("Healthcheck attempt {} returned HTTP code: {}", attempt, code);
                logService.info(deploymentId, "Connection attempt " + attempt + " succeeded. Application responded.", LogCategory.RUNTIME);
                connection.disconnect();
                return; // Health check passed!
            } catch (Exception e) {
                // Connection refused or timed out, wait and retry
                log.debug("Healthcheck connection failed: {}", e.getMessage());
                TimeUnit.MILLISECONDS.sleep(1500);
                attempt++;
            }
        }

        logService.error(deploymentId, "Application health checks failed. Port is unresponsive after 45 seconds.", LogCategory.RUNTIME);
        throw new RuntimeException("Container healthcheck timeout. Application is not responding on host port: " + port);
    }
}
