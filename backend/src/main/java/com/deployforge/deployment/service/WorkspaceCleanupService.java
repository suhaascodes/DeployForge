package com.deployforge.deployment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

@Service
@Slf4j
public class WorkspaceCleanupService {

    private static final String WORKSPACE_BASE = "workspaces";

    public void cleanupWorkspace(UUID deploymentId) {
        Path workspacePath = Paths.get(WORKSPACE_BASE, deploymentId.toString());
        File workspaceDir = workspacePath.toFile();

        if (workspaceDir.exists()) {
            log.info("Starting workspace cleanup for deployment: {}", deploymentId);
            try {
                Files.walk(workspacePath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Workspace folders wiped successfully for deployment: {}", deploymentId);
            } catch (Exception e) {
                log.error("Failed to clean up workspace for deployment: {}", deploymentId, e);
            }
        }
    }
}
