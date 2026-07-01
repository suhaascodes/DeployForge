package com.deployforge.deployment.build;

import com.deployforge.deployment.entity.ProjectType;
import com.deployforge.logging.entity.LogCategory;
import com.deployforge.logging.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BuildExecutionService {

    private final LogService logService;
    private static final long DEFAULT_TIMEOUT_MINUTES = 10;

    public BuildExecutionService(LogService logService) {
        this.logService = logService;
    }

    public void executeBuild(String deploymentId, File workspaceDir, ProjectType projectType) throws Exception {
        logService.info(deploymentId, "Starting build compilation step for framework: " + projectType.name(), LogCategory.BUILD);

        switch (projectType) {
            case REACT:
                runCommand(deploymentId, workspaceDir, "npm", "install");
                runCommand(deploymentId, workspaceDir, "npm", "run", "build");
                break;
            case SPRING_BOOT:
                runCommand(deploymentId, workspaceDir, "mvn", "clean", "package", "-DskipTests");
                break;
            case NODE:
                runCommand(deploymentId, workspaceDir, "npm", "install");
                break;
            case STATIC_HTML:
                logService.info(deploymentId, "Static HTML repository detected. No compile steps needed. Skipping build step.", LogCategory.BUILD);
                break;
            default:
                throw new IllegalArgumentException("Unsupported project framework: " + projectType);
        }

        logService.info(deploymentId, "Build compilation completed successfully", LogCategory.BUILD);
    }

    private void runCommand(String deploymentId, File directory, String... command) throws Exception {
        log.info("Executing process command: {} inside directory: {}", String.join(" ", command), directory.getAbsolutePath());
        logService.info(deploymentId, "Running shell task: " + String.join(" ", command), LogCategory.BUILD);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(directory);
        pb.redirectErrorStream(true); // Merge stdout and stderr for easy reading

        Process process = pb.start();

        // Read output in real-time
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logService.info(deploymentId, line, LogCategory.BUILD);
            }
        }

        boolean completed = process.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            logService.error(deploymentId, "Process timeout exceeded (" + DEFAULT_TIMEOUT_MINUTES + " mins). Killing build.", LogCategory.BUILD);
            throw new RuntimeException("Build command execution timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            logService.error(deploymentId, "Build command failed with exit code: " + exitCode, LogCategory.BUILD);
            throw new RuntimeException("Build command failed with non-zero exit code: " + exitCode);
        }
    }
}
