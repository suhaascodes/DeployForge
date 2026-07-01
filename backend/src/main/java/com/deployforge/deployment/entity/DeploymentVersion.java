package com.deployforge.deployment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deployment_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false, unique = true)
    private Deployment deployment;

    @Column(name = "git_commit_hash", nullable = false, length = 40)
    private String gitCommitHash;

    @Column(name = "version_tag", length = 100)
    private String versionTag;

    @CreationTimestamp
    @Column(name = "deployed_at", nullable = false, updatable = false)
    private LocalDateTime deployedAt;
}
