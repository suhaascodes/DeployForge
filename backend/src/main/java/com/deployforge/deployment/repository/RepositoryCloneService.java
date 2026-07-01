package com.deployforge.deployment.repository;

import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentVersion;
import com.deployforge.deployment.repository.DeploymentVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
@Slf4j
public class RepositoryCloneService {

    private final DeploymentVersionRepository deploymentVersionRepository;
    private static final String WORKSPACE_BASE = "workspaces";

    public RepositoryCloneService(DeploymentVersionRepository deploymentVersionRepository) {
        this.deploymentVersionRepository = deploymentVersionRepository;
    }

    public File cloneRepository(Deployment deployment) throws Exception {
        String repoUrl = deployment.getProject().getRepositoryUrl();
        File workspaceDir = Paths.get(WORKSPACE_BASE, deployment.getId().toString()).toFile();

        log.info("Cloning Git repository: {} to isolated workspace: {}", repoUrl, workspaceDir.getAbsolutePath());

        // Call JGit clone command
        try (Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(workspaceDir)
                .setCloneAllBranches(true)
                .call()) {

            Repository jgitRepo = git.getRepository();
            String branchName = jgitRepo.getBranch();
            ObjectId head = jgitRepo.resolve("HEAD");
            String commitHash = head != null ? head.getName() : "unknown";

            log.info("Successfully checked out branch: {} at commit: {}", branchName, commitHash);

            // Persist the resolved version metadata
            DeploymentVersion version = DeploymentVersion.builder()
                    .deployment(deployment)
                    .gitCommitHash(commitHash)
                    .versionTag(branchName)
                    .deployedAt(LocalDateTime.now())
                    .build();
            deploymentVersionRepository.save(version);

            return workspaceDir;
        }
    }
}
