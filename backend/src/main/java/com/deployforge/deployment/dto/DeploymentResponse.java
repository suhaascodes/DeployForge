package com.deployforge.deployment.dto;

import com.deployforge.deployment.entity.Deployment;
import com.deployforge.deployment.entity.DeploymentRuntime;
import com.deployforge.deployment.entity.DeploymentStatus;
import com.deployforge.deployment.entity.DeploymentVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private DeploymentStatus status;
    private String deploymentUrl;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String gitCommitHash;
    private String versionTag;
    
    // V2 Container Info
    private String containerId;
    private String containerName;
    private String imageTag;
    private Integer hostPort;
    private Long buildDurationMs;
    private String frameworkDetected;

    public static DeploymentResponse fromEntity(Deployment deployment, DeploymentVersion version, DeploymentRuntime runtime) {
        Long duration = null;
        if (deployment.getBuildStartedAt() != null && deployment.getBuildCompletedAt() != null) {
            duration = java.time.Duration.between(deployment.getBuildStartedAt(), deployment.getBuildCompletedAt()).toMillis();
        }

        return DeploymentResponse.builder()
                .id(deployment.getId())
                .projectId(deployment.getProject().getId())
                .projectName(deployment.getProject().getName())
                .status(deployment.getStatus())
                .deploymentUrl(deployment.getDeploymentUrl())
                .startedAt(deployment.getStartedAt())
                .completedAt(deployment.getCompletedAt())
                .gitCommitHash(version != null ? version.getGitCommitHash() : null)
                .versionTag(version != null ? version.getVersionTag() : null)
                .containerId(runtime != null ? runtime.getContainerId() : null)
                .containerName(runtime != null ? runtime.getContainerName() : null)
                .imageTag(runtime != null ? runtime.getImageTag() : null)
                .hostPort(runtime != null ? runtime.getHostPort() : null)
                .buildDurationMs(duration)
                .frameworkDetected(deployment.getFrameworkDetected())
                .build();
    }

    public static DeploymentResponse fromEntity(Deployment deployment, DeploymentVersion version) {
        return fromEntity(deployment, version, null);
    }
}
