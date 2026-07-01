package com.deployforge.deployment.entity;

import com.deployforge.project.entity.Project;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deployments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeploymentStatus status;

    @Column(name = "deployment_url")
    private String deploymentUrl;

    @Column(name = "build_started_at")
    private LocalDateTime buildStartedAt;

    @Column(name = "build_completed_at")
    private LocalDateTime buildCompletedAt;

    @Column(name = "framework_detected")
    private String frameworkDetected;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private DeploymentTriggerType triggerType = DeploymentTriggerType.MANUAL;

    @Column(name = "github_commit_hash", length = 40)
    private String githubCommitHash;

    @Column(name = "github_commit_message")
    private String githubCommitMessage;

    @Column(name = "github_author")
    private String githubAuthor;

    @Column(name = "github_push_timestamp")
    private LocalDateTime githubPushTimestamp;

    @Column(name = "deployment_duration_ms")
    private Long deploymentDurationMs;

    @Column(name = "image_size_mb")
    private Double imageSizeMb;

    @Column(name = "source_deployment_id")
    private UUID sourceDeploymentId;

    @Column(name = "failure_stage", length = 50)
    private String failureStage;

    @Column(name = "failure_summary")
    private String failureSummary;
}
