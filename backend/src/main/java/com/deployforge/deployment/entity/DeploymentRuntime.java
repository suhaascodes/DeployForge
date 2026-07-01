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
@Table(name = "deployment_runtime")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRuntime {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "deployment_id", nullable = false)
    private UUID deploymentId;

    @Column(name = "container_id")
    private String containerId;

    @Column(name = "container_name")
    private String containerName;

    @Column(name = "image_tag")
    private String imageTag;

    @Column(name = "host_port")
    private Integer hostPort;

    @Enumerated(EnumType.STRING)
    @Column(name = "runtime_status", nullable = false)
    private RuntimeStatus runtimeStatus;

    @Column(name = "image_created_at", nullable = false)
    private LocalDateTime imageCreatedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
